package com.kaikeba.k8s.stages

import java.io.Serializable
import org.yubing.delivery.Stage

/**
 *	K8s滚动部署
 */
class ConfirmDeployOKStage extends Stage {
	def serverName
	def version
	def script
	def config
	def deployEnv
	ConfirmDeployOKStage(project, stageName, deployEnv) {
		super(project, stageName)

		this.script = project.script
		this.config = project.config
		this.deployEnv = deployEnv

	}

	def confirmDeployResult() {
		def result = this.script.input message: '上线结果:', parameters: [this.script.choice(choices: ['上线成功,弹冠相庆✌️', '赶紧回滚,不然滚蛋😭'], description: '上线结果', name: 'DEPLOY_RESULT')]
		this.script.echo "input result: ${result}"
		if (result == '上线成功,弹冠相庆✌️'){
			return true
		} else {
			return false
		}
	}

	def run() {
        def serverName = this.config.name
        def buildNo = this.script.env.BUILD_NUMBER
        def branch = this.script.env.gitlabSourceBranch.replace('origin/','')

        // time out auto merge
        try {
            this.script.timeout( time: 1, unit: 'HOURS'){
                if (confirmDeployResult()){
                    this.script.env.DEPLOY_RESULT="${buildNo}.ok"
                } else {
                    this.script.env.DEPLOY_RESULT="${buildNo}.undo"
                }
          }
        } catch (oneHourTimeoutErr) {
            this.script.echo """${oneHourTimeoutErr}"""
            this.script.node('jenkins-slave-k8s'){
              sendToDingTalk(serverName,branch,buildNo,"17801036141", "上线超过1个小时未合并，请及时合并")
            }
            try {
                this.script.timeout( time: 1, unit: 'HOURS' ){
                    if (confirmDeployResult()){
                        this.script.env.DEPLOY_RESULT="${buildNo}.ok"
                    } else {
                        this.script.env.DEPLOY_RESULT="${buildNo}.undo"
                    }
              }
            } catch (twoHoursTimeoutErr) {
                this.script.node('jenkins-slave-k8s'){
                  sendToDingTalk(serverName,branch,buildNo,"17801036141", "上线超过2个小时未合并，将自动合并")
                }
                this.script.env.DEPLOY_RESULT="${buildNo}.ok"
            }
        }
    }
    def sendToDingTalk(serverName, branch, buildNo, mobile, message){
      def notify = this.config.notify
      def notify_shell = """          curl -X POST 'https://oapi.dingtalk.com/robot/send?access_token=b59d993a7e39b9e92eb7d737966b7fa255d07df499f3f6d70cbe92dc54973e27' \
          -H 'Content-Type: application/json' \
          -d '{"msgtype": "text","text": {"content": "${serverName} ${branch}分支.\n ${message}!\n${this.script.env.BUILD_URL}\n@${mobile}"},"at":{"atMobiles":["${mobile}"],"isAtall":false}}'"""
      if (notify) {
        this.script.sh notify_shell
      }
    }
}
