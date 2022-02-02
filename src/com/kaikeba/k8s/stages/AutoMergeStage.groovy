package com.kaikeba.k8s.stages

import java.io.Serializable
import org.yubing.delivery.Stage

/**
 *    自动合并分支
 */
class AutoMergeStage extends Stage {
    def serverName
    def version
    def commitId
    def branch_origin
    def branch
    def script
    def config
    AutoMergeStage(project, stageName) {
        super(project, stageName)
        this.script = project.script
        this.config = project.config
        this.branch_origin = project.env.BRANCH_NAME
    }

    def run() {
        this.serverName = config.name
        this.version = config.version
        this.commitId = version[-6..-1]
        this.branch = branch_origin
        // BRANCH_NAME 
        if (branch_origin.contains('origin/')) {
            // 修复branch name错误，删除origin前缀
            this.branch = branch_origin[7..-1]
        }
        this.script.echo "branch is ${branch}"

        this.merge()
    }

    def merge() {
        def version = this.version
        def buildNo = this.script.env.BUILD_NUMBER
        if (this.script.env.DEPLOY_RESULT == "${buildNo}.undo"){
            this.script.echo "回滚不触发合并"
        } else {
            this.script.node('jenkins-slave-hc32') {
                this.script.echo "login to jenkins-slave-hc32"

                this.script.checkout this.script.scm
                this.script.sh "  git config --local user.email ykqin@kaikeba.com"
                this.script.sh "  git config --local user.name ykqin"
                // this.script.sh "git fetch"

                // this.script.echo "${branch}"

                // 获取当前分支自动合并分支
                this.script.sshagent(['ykqin-devops']) {

                    if (branch == "master") {
                        this.script.sh "git checkout -B master --track origin/master"

                        this.script.sh "git reset --hard ${commitId}"

                        this.script.sh "git checkout -B develop --track origin/develop"

                        this.script.sh "git merge master"

                        this.script.sh "git push origin develop"

                        this.script.echo "Merged branch master to develop success!"
                    } else if (branch == "develop") {
                        this.script.sh "git checkout -B develop --track origin/develop"

                        this.script.sh "git reset --hard ${commitId}"

                        this.script.sh "git checkout -B master --track origin/master"

                        this.script.sh "git merge develop"

                        this.script.sh "git push origin master"

                        this.script.echo "Merged branch develop to master success!"
                    } else if (branch.startsWith('release/') || branch.startsWith('fix/')||branch.startsWith('hotfix/')||branch.startsWith('bugfix/') ) {
                        def tag = branch.replace('release/','').replace('hotfix/','').replace('bugfix/','').replace('fix/','')
                        this.script.sh "git checkout -B ${branch} --track origin/${branch}"

                        this.script.sh "git reset --hard ${commitId}"

                        this.script.sh "git checkout -B master --track origin/master"

                        this.script.sh "git merge ${branch}"

                        this.script.sh "git tag -m '${tag}' ${tag}"

                        this.script.sh "git push origin master"

                        this.script.echo "Merged branch ${branch} to master success!"
                        this.script.sh "git push --tags"

                        this.script.echo "Merged tag ${tag} to origin success!"

//                        this.script.sh "git checkout -B develop --track origin/develop"
//
//                        this.script.sh "git merge ${branch}"
//
//                        this.script.sh "git push origin develop"
//
//                        this.script.echo "Merged branch ${branch} to develop success!"


                    } else {
                        this.script.echo "Please make sure your branch is develop or master or release/xxx or fix/xxx!"
                    }
                }
            }
        }
    }
}
