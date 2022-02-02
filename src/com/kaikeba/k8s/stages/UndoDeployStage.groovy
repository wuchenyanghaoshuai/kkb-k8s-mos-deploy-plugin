package com.kaikeba.k8s.stages

import java.io.Serializable
// import org.yubing.delivery.Stage
import com.kaikeba.k8s.stages.PullScriptStage

/**
 *	K8s滚动部署
 */
class UndoDeployStage extends PullScriptStage {
	def serverName
	def version
	def deployEnv
	def script
	def config
	UndoDeployStage(project, stageName, deployEnv) {
		super(project, stageName)

		this.script = project.script
		this.config = project.config
		this.deployEnv = deployEnv
	}


	def run() {
		this.serverName = this.config.name
		this.version = this.config.version
		def buildNo = this.script.env.BUILD_NUMBER
		def deployNode = 'jenkins-slave-k8s'
		if (this.script.env.DEPLOY_RESULT == "${buildNo}.undo"){
			undoDeploy(deployNode)
			this.script.echo "I am so sad to do so"
		} else if (this.script.env.DEPLOY_RESULT == "${buildNo}.ok"){
			this.script.echo "Celebrate with a beer!"
		}
	}

	def undoDeploy(deployNode) {
		def deployEnv = this.deployEnv
		this.script.echo "config:${this.config}"
		def deploy_name = this.deployName("prod")
		def sub_dir = this.config.subdir
    if (sub_dir ==null || sub_dir==""){
          sub_dir="."
        }
    def nameSpace = "mos-prod"
      def map = this.config.k8s_namespaces
      def category = this.config.category
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
		this.script.node(deployNode) {
			// 回滚prod环境
			def serverEnv = []
			serverEnv.add("K8S_OPTS= --kubeconfig=/root/.kube/kkb-prod/kubectl_config_prod ")
			serverEnv.add("K8S_NAME_SPACE=${nameSpace}")
			serverEnv.add("K8S_DEPLOY_YML=" + "${sub_dir}" + "/release/k8s/prod/deploy.yml")
			serverEnv.add("K8S_SVC_YML=" + "${sub_dir}" + "/release/k8s/prod/svc.yml")
			serverEnv.add("K8S_DEPLOY_NAME=" + "${deploy_name}")

			this.script.echo "UndoDeployStage serverEnv:${serverEnv}"
			this.script.echo "UndoDeployStage subdir:${sub_dir}"

			this.script.withEnv(serverEnv) {
				this.script.sh "kubectl \$K8S_OPTS get deploy -n \$K8S_NAME_SPACE \$K8S_DEPLOY_NAME -o yaml>/data/app/jenkins/deploy_backups/\$K8S_DEPLOY_NAME.\$K8S_NAME_SPACE.\$(date +%Y%m%d_%H%M%S).undo.yaml"
				this.script.sh "${deploy_script_dir}/${sub_dir}/undo.sh"
			}
		}
	}


}

