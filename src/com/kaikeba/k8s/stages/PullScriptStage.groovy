package com.kaikeba.k8s.stages

import org.yubing.delivery.Stage

class PullScriptStage extends Stage {

    public def script
    public String deploy_script_dir
		def config
    PullScriptStage(project, stageName) {
        super(project, stageName)

        this.script = project.script
        this.config = project.config
        this.deploy_script_dir = "cloud-config-mos"
    }

    def pullScript() {
        this.script.echo "start pull deploy script"
        def release_repo = config.release_repo
				if (release_repo =="" || release_repo == null) {
					release_repo='kkb-cloud'
				}
        this.script.dir("${deploy_script_dir}") {
            this.script.git([
                    url          : "git@192.168.1.17:kkb-release/${release_repo}.git",
                    // url          : "git@192.168.100.11:kkb-release/kkb-cloud1/${release_repo}.git",
                    branch       : "master",
                    credentialsId: 'bb78c5fd-8117-49f9-866a-62427dbfcedc'
            ])
        }

        def configCommitId = this.script.sh(returnStdout: true, script: "cd ${this.deploy_script_dir} && git rev-parse HEAD | tr -d '\n'")
        this.script.echo "end pull deploy script,configCommitId: ${configCommitId}"

        return configCommitId
    }

    def deployName(deployEnv) {
        def name = this.config.name
        def map = this.config.k8s_deploy_names

        if (map && map["${deployEnv}"]) {
            name = map["${deployEnv}"]
        }

        return name
    }

}
