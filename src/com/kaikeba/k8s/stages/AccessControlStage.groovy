package com.kaikeba.k8s.stages

import org.yubing.delivery.Stage

/**
 *	确认消息
 */
class AccessControlStage extends Stage {
	def version
	def commitId

	def script
	def config
	def deployEnv

	AccessControlStage(project, stageName, deployEnv) {
		super(project, stageName)

		this.script = project.script
		this.config = project.config
		this.deployEnv = deployEnv
	}

	def getUserId(){
		if (this.script.env.CHANGE_AUTHOR){
			return this.script.env.CHANGE_AUTHOR
		}
		return this.script.env.USER_ID
	}

	def getLatestCommit(){
		this.script.checkout this.script.scm
		def commitid = this.script.sh(returnStdout: true, script: "git rev-parse HEAD")
		return commitid
	}

	def run() {
		this.version = config.version
		this.commitId = config.gitCommitId

		this.script.node('jenkins-slave-k8s') {
			this.innerRun()
		}
	}

	def innerRun() {
		def userId = getUserId()
		def gitlabBranch = this.script.env.gitlabBranch
		def gitlabActionType = this.script.env.gitlabActionType

		// 权限白名单

		def ciWhiteList = ['devops']
		def gitlabWhiteList = []
		if (this.deployEnv == 'dev'){
			ciWhiteList.add('kkb-mos-dev')
			ciWhiteList.add('kkb-mos-test')
			ciWhiteList.add('devops-kkb')
			ciWhiteList.add('hky-bi')
			ciWhiteList.add('datagration')
			ciWhiteList.add('kkb-talent')
            ciWhiteList.add('kkb-lumiere')
		} else if (this.deployEnv == 'test'){
			ciWhiteList.add('kkb-mos-test')
			ciWhiteList.add('devops')
            ciWhiteList.add('kkb-opinion')
            ciWhiteList.add('kkb-mos-dev')
            ciWhiteList.add('datagration')
            ciWhiteList.add('kkb-xk-test')
			ciWhiteList.add('kkb-talent')
			ciWhiteList.add('kkb-lumiere')
            ciWhiteList.add('hky-bi')
		} else if (this.deployEnv == 'pre'){
			ciWhiteList.add('kkb-xk-test')
			ciWhiteList.add('kkb-mos-test')
			ciWhiteList.add('devops')
            ciWhiteList.add('kkb-lumiere')	
            ciWhiteList.add('kkb-pre')
		} else if (this.deployEnv == 'prod'){
			ciWhiteList.add('devops')
			ciWhiteList.add('hky-bi')
		}

    def projectWhiteList = ["kkb-finance-bi-web-server ", "kkb-competitor-analytics-web-server","big-data-bi-api", "big-data-bi-dingtalk-api"]
		projectWhiteList.each{  
			if (this.script.env.BUILD_URL.contains(it)) {
				ciWhiteList.add('hky-bi')
			}
		}
		this.script.echo "ciWhiteList:${ciWhiteList}"

		def whiteList = ciWhiteList + gitlabWhiteList
		// 校验用户权限
		if (whiteList.contains(userId)){
			this.script.echo "userid: ${userId},权限检验成功,准备部署${this.deployEnv}"
		} else {
			this.script.echo "userid: ${userId},账号权限校验失败，终止部署${this.deployEnv}"
			this.script.sh 'exit 1'
		}

		// gitlabWhiteList中的用户触发的构建只能上线develop或master分支
		// 校验最新代码
		//if (this.commitId != getLatestCommit()){
		//	this.script.input message: '当前上线版本非最新commitid,是否确认上线？'
		//}
	}
}
