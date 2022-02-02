package com.kaikeba.k8s.stages

import com.kaikeba.k8s.stages.PullScriptStage
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions

/**
 * 部署QA2环境
 */
@GrabResolver(name='aliyun', root='http://maven.aliyun.com/nexus/content/groups/public/')
@Grab('org.yaml:snakeyaml:1.17')
class QA2DeployStage extends PullScriptStage {


    def config
    def backup
    QA2DeployStage(project, stageName, backup) {

        super(project, stageName)

        this.config = project.config
        this.backup = backup
    }

    def run() {
        this.script.echo "config:${this.config}"

        def deploy_name = this.deployName("test")
        def sub_dir = this.config.subdir
        def version = this.config.version
        def newArch = this.config.newArch
        if (sub_dir ==null || sub_dir==""){
          sub_dir="."
        }
        this.script.node('jenkins-slave-k8s') {
            this.script.checkout this.script.scm

            // 拉取脚本
            pullScript()

            def imageName = this.config.name
            def nameSpace = "kkb-test2"
            def map = this.config.k8s_namespaces
						def category = this.config.category
						def deployEnv = 'test2'
            if (map) {
              def tempNameSpace = map["${deployEnv}"]
              if (tempNameSpace) {
                nameSpace = tempNameSpace
              } else if (category) {
                nameSpace = "kkb-${category}-${deployEnv}"
              }
            } else if (category) {
              nameSpace = "kkb-${category}-${deployEnv}"
            }
            // 部署测试环境
            def serverEnv = []
            serverEnv.add("K8S_OPTS= --kubeconfig=/root/.kube/kkb-test/kubectl_config_test")
            serverEnv.add("K8S_NAME_SPACE=${nameSpace}")
            serverEnv.add("K8S_DEPLOY_YML=" + "${deploy_script_dir}/${sub_dir}" + "/k8s/test2/deploy.yml")
            serverEnv.add("K8S_SVC_YML=" + "${deploy_script_dir}/${sub_dir}" + "/k8s/test2/svc.yml")
            serverEnv.add("K8S_DEPLOY_NAME=" + "${deploy_name}")
            serverEnv.add("DOCKER_TAG=" + "${version}")

            this.script.echo "QA2DeployStage serverEnv:${serverEnv}"

            this.script.withEnv(serverEnv) {
                if (this.backup){
                    try {
                       this.script.sh "kubectl \$K8S_OPTS get deploy -n \$K8S_NAME_SPACE \$K8S_DEPLOY_NAME -o yaml>/data/app/jenkins/deploy_backups/\$K8S_DEPLOY_NAME.\$K8S_NAME_SPACE.\$(date +%Y%m%d_%H%M%S).yaml"
                    } catch(e) {
                      this.script.echo "backup fail::${e.message}"
                    }
                }
                
                def yamlfile = this.script.sh(returnStdout: true, script:"cat ${this.script.env.WORKSPACE}/${deploy_script_dir}/${sub_dir}/k8s/test2/deploy.yml")
                this.script.sh "echo '----------------------------'"
                def output = this.addStartTime(yamlfile)
                this.script.sh("echo \"\"\"${output}\"\"\">${deploy_script_dir}/${sub_dir}/k8s/test2/deploy.yml")
                this.script.sh "${deploy_script_dir}/${sub_dir}/deploy.sh"
            }
        }
    }

    @NonCPS
    def addStartTime(yamlfile) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options)
        Map deploy = yaml.load(yamlfile)
        def now = new Date()
        deploy.spec.template.spec.containers[0].env.add([name: "START", value: now.getDateTimeString() ])
        String output = yaml.dump(deploy);
        return output;
    }
}

