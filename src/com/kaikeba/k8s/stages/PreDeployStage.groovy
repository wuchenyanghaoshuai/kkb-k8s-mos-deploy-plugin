package com.kaikeba.k8s.stages

import com.kaikeba.k8s.stages.PullScriptStage
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions

/**
 * pre
 */
@GrabResolver(name='aliyun', root='http://maven.aliyun.com/nexus/content/groups/public/')
@Grab('org.yaml:snakeyaml:1.17')
class PreDeployStage extends PullScriptStage {

    def config
    def backup
    PreDeployStage(project, stageName, backup) {
        super(project, stageName)
        this.config = project.config
        this.backup = backup
    }

    def run() {
        this.script.echo "config:${this.config}"

        def deploy_name = this.deployName("pre")
        def sub_dir = this.config.subdir
        def version = this.config.version

        def newArch = this.config.newArch
//        if (sub_dir == ""||sub_dir == null||newArch) {
        if (sub_dir == ""||sub_dir == null) {
          sub_dir="."
        }


        this.script.node('jenkins-slave-k8s') {
            this.script.checkout this.script.scm

            // ÊãâÂèñËÑöÊú¨
            pullScript()

            def imageName = this.config.name
            def nameSpace = "mos-pre"
            def map = this.config.k8s_namespaces
						def category = this.config.category
						def deployEnv = 'pre'
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
            if (imageName=='kkb-bi-api'){
              imageName="bi-api"
            }
            // ÈÉ®ÁΩ≤preÁéØÂ¢É
            def serverEnv = []
            serverEnv.add("K8S_OPTS= --kubeconfig=/root/.kube/kkb-pre/kubectl_config_pre ")
            serverEnv.add("K8S_NAME_SPACE=${nameSpace}")
            serverEnv.add("K8S_DEPLOY_YML=" + "${deploy_script_dir}/${sub_dir}" + "/k8s/pre/deploy.yml")
            serverEnv.add("K8S_SVC_YML=" + "${deploy_script_dir}/${sub_dir}" + "/k8s/pre/svc.yml")
            serverEnv.add("K8S_DEPLOY_NAME=" + "${deploy_name}")
            serverEnv.add("DOCKER_TAG=" + "${version}")
            serverEnv.add("DOCKER_IMAGE_NAME=" + "${imageName}")

            def backupDate = this.script.sh(returnStdout: true, script: "date +%Y%m%d_%H%M%S| tr -d '\n'")
            serverEnv.add("BACKUP_DATE=" + "${backupDate}")
            this.script.echo "PreDeployStage serverEnv:${serverEnv}"
            def firstBlood = false
            this.script.withEnv(serverEnv) {
              try {
                if (this.backup){
                    this.script.sh "kubectl \$K8S_OPTS get deploy -n \$K8S_NAME_SPACE \$K8S_DEPLOY_NAME -o yaml>/data/app/jenkins/deploy_backups/\$K8S_DEPLOY_NAME.\$K8S_NAME_SPACE.\$BACKUP_DATE.yaml"
                }
              } catch (e) {
                firstBlood = true
                this.script.echo "deploy ${deploy_name} not found, may be first deploy"
              }
                def yamlfile = this.script.sh(returnStdout: true, script:"cat ${this.script.env.WORKSPACE}/${deploy_script_dir}/${sub_dir}/k8s/pre/deploy.yml")
                this.script.sh "echo '----------------------------'"
                def output = this.addStartTime(yamlfile)
                this.script.sh("echo \"\"\"${output}\"\"\">${deploy_script_dir}/${sub_dir}/k8s/pre/deploy.yml")

                this.script.sh "${deploy_script_dir}/${sub_dir}/rollout.sh"
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


