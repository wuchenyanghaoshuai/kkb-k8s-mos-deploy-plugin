package com.kaikeba.k8s.stages

import com.kaikeba.k8s.stages.PullScriptStage

/**
 * 部署QA环境
 */
class DevDeployStage extends PullScriptStage {


    def config
    def backup
    DevDeployStage(project, stageName, backup) {

        super(project, stageName)

        this.config = project.config
        this.backup = backup
    }

    def run() {
        this.script.echo "config:${this.config}"

        def deploy_name = this.deployName("test")
        def sub_dir = this.config.subdir
        def version = this.config.version
        if (sub_dir ==null || sub_dir==""){
          sub_dir="."
        }
        this.script.node('jenkins-slave-k8s') {
            this.script.checkout this.script.scm

            // 拉取脚本
            pullScript()
            // 部署测试环境
            def serverEnv = []
            serverEnv.add("K8S_OPTS= --kubeconfig=/root/.kube/kkb-test/kubectl_config_test")
            serverEnv.add("K8S_NAME_SPACE=kkb-dev")
            serverEnv.add("K8S_DEPLOY_YML=" + "${deploy_script_dir}/${sub_dir}" + "/k8s/dev/deploy.yml")
            serverEnv.add("K8S_SVC_YML=" + "${deploy_script_dir}/${sub_dir}" + "/k8s/dev/svc.yml")
            serverEnv.add("K8S_DEPLOY_NAME=" + "${deploy_name}")
            serverEnv.add("DOCKER_TAG=" + "${version}")

            this.script.echo "QADeployStage serverEnv:${serverEnv}"

            this.script.withEnv(serverEnv) {
                if (this.backup){
                    try {
                      this.script.sh "kubectl \$K8S_OPTS get deploy -n \$K8S_NAME_SPACE \$K8S_DEPLOY_NAME -o yaml>/data/app/jenkins/deploy_backups/\$K8S_DEPLOY_NAME.\$K8S_NAME_SPACE.\$(date +%Y%m%d_%H%M%S).yaml"
                    } catch(e) {
                      this.script.echo "backup fail::${e.message}"
                    }
                }
                this.script.sh "${deploy_script_dir}/${sub_dir}/deploy.sh"
            }
        }
    }

}

