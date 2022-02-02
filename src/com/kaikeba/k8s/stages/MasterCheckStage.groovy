package com.kaikeba.k8s.stages

import com.kaikeba.k8s.stages.PullScriptStage

/**
 * 判断上线分支是否包含master最新代码
 */
class MasterCheckStage extends PullScriptStage {


	def config
	def deployEnv
	MasterCheckStage(project, stageName, deployEnv) {
		super(project, stageName)
		this.config = project.config
		this.deployEnv = deployEnv
	}

	def run() {
		def serverName = this.config.name
		def version = this.config.version
		this.script.echo "config:${this.config}"
		this.script.currentBuild.description="${this.script.params.CHANGE_TYPE}:${this.script.env.gitlabSourceBranch}"
		def branch = this.script.env.gitlabSourceBranch
		this.script.node('jenkins-slave-k8s') {
			this.script.checkout this.script.scm

			this.script.sh """
               git checkout master
               git pull origin master
               git branch
         """

			def gitdiff = this.script.sh(returnStdout: true, script: "git log master ^${this.script.env.gitlabSourceBranch}")

			if (gitdiff == ""){
				this.script.sh "exit 0"
			} else {
				this.script.node('jenkins-slave-k8s'){
					sendToDingTalk(serverName,branch,"18210181208", "上线分支没有包含master最新代码!!!")
					this.script.sh "exit 1"
				}
//             判断是否继续上线
//             if (confirmDeployResult()){
//                 this.script.sh "echo 上线分支没有包含master最新代码，选择继续上线代码"
//             } else {
//                 this.script.sh "exit 1"
//             }
			}
		}
	}

	def confirmDeployResult() {
		def result = this.script.input message: '是否上线:', parameters: [this.script.choice(choices: ['continue', 'review代码'], description: '上线结果', name: 'DEPLOY_RESULT')]
		this.script.echo "input result: ${result}"
		if (result == 'continue'){
			return  true
		} else {
			return false
		}
	}

	def sendToDingTalk(serverName, branch, mobile, message){
		def notify_shell = """curl -X POST 'https://oapi.dingtalk.com/robot/send?access_token=c38040bda850d9f2e6b53efd2bb7be7b943dea64db4497054e80575b437e2315' \
          -H 'Content-Type: application/json' \
          -d '{"msgtype": "text","text": {"content":"项目名称:${serverName}\n分支:${branch}\n消息:${message}\n详情:${this.script.env.BUILD_URL}console\n@${mobile}"},"at":{"atMobiles":["${mobile}"],"isAtall":false}}'"""
		this.script.sh notify_shell
	}

}

