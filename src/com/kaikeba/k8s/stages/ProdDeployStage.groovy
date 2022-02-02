package com.kaikeba.k8s.stages

import com.kaikeba.k8s.stages.PullScriptStage
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions

/**
 * 部署Prod环境
 */
@GrabResolver(name='aliyun', root='http://maven.aliyun.com/nexus/content/groups/public/')
@Grab('org.yaml:snakeyaml:1.17')
class ProdDeployStage extends PullScriptStage {

    def config
    def backup
    ProdDeployStage(project, stageName, backup) {
        super(project, stageName)
        this.config = project.config
        this.backup = backup
    }

    def run() {
        this.script.echo "config:${this.config}"

        def deploy_name = this.deployName("prod")
        def sub_dir = this.config.subdir
        def version = this.config.version
        def newArch = this.config.newArch
				if (sub_dir == ""||sub_dir == null||newArch) {

          sub_dir="."
        }


        this.script.node('jenkins-slave-k8s') {
            this.script.checkout this.script.scm

            // 拉取脚本
            pullScript()

            def imageName = this.config.name
            def nameSpace = "mos-prod"
            def map = this.config.k8s_namespaces
						def category = this.config.category
						def deployEnv = 'prod'
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
            // 部署prod环境
            def serverEnv = []
            serverEnv.add("K8S_OPTS= --kubeconfig=/root/.kube/kkb-prod/kubectl_config_prod ")
            serverEnv.add("K8S_NAME_SPACE=${nameSpace}")
            serverEnv.add("K8S_DEPLOY_YML=" + "${deploy_script_dir}/${sub_dir}" + "/k8s/prod/deploy.yml")
            serverEnv.add("K8S_SVC_YML=" + "${deploy_script_dir}/${sub_dir}" + "/k8s/prod/svc.yml")
            serverEnv.add("K8S_DEPLOY_NAME=" + "${deploy_name}")
            serverEnv.add("DOCKER_TAG=" + "${version}")
            serverEnv.add("DOCKER_IMAGE_NAME=" + "${imageName}")

            def backupDate = this.script.sh(returnStdout: true, script: "date +%Y%m%d_%H%M%S| tr -d '\n'")
            serverEnv.add("BACKUP_DATE=" + "${backupDate}")
            this.script.echo "ProdDeployStage serverEnv:${serverEnv}"
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
            if (!firstBlood) {

                    def prodCommitId = this.script.sh(returnStdout: true, script: """ grep 'image:' /data/app/jenkins/deploy_backups/\$K8S_DEPLOY_NAME.\$K8S_NAME_SPACE.\$BACKUP_DATE.yaml |grep \$DOCKER_IMAGE_NAME |awk  -F ':' '{print \$3}'|cut -d '-' -f 2|tr -d '\n'""")
                    def prodCommitIdExist = this.script.sh(returnStdout: true, script: """git log --pretty=format:"%H" --since=500.months  |grep ${prodCommitId}|tr -d '\n'""")
                    if (prodCommitIdExist==""){
                        this.script.timeout(time: 1, unit: "HOURS"){
                        this.script.input message: "线上commitId:${prodCommitId}在当前分支不存在，继续上线可能发生功能回退！请仔细确认！"
                        }
                    }else{
                      this.script.echo "线上commitId:${prodCommitId}在当前分支存在"
                    }

            }
                def yamlfile = this.script.sh(returnStdout: true, script:"cat ${this.script.env.WORKSPACE}/${deploy_script_dir}/${sub_dir}/k8s/prod/deploy.yml")
                this.script.sh "echo '----------------------------'"
                def output = this.addStartTime(yamlfile)
                this.script.sh("echo \"\"\"${output}\"\"\">${deploy_script_dir}/${sub_dir}/k8s/prod/deploy.yml")

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

