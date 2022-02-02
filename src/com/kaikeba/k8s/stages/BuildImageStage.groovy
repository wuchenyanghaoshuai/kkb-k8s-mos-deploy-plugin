package com.kaikeba.k8s.stages

import com.kaikeba.k8s.stages.PullScriptStage

/**
 * 构建mos镜像
 */
class BuildImageStage extends PullScriptStage {

    def config
    def imageName
    def version
    def script
    def deployEnv
    def registryUrl
    def registryLogIn
    def registryNS
    def newArch
    def subDir
    BuildImageStage(project, stageName, deployEnv) {
        super(project, stageName)
        this.config = project.config
        this.script = project.script
        this.deployEnv = deployEnv
    }

    def buildImage(){
      this.script.docker.withRegistry("http://${this.registryUrl}", this.registryLogIn) {
         def serverEnv = []
         serverEnv.add("DOCKER_REGISTRY_URL=${this.registryUrl}")
         serverEnv.add("DOCKER_NAME_SPACE=${this.registryNS}")
         serverEnv.add("DOCKER_IMAGE_NAME=" + "${this.imageName}")
         serverEnv.add("DOCKER_TAG=" + "${this.version}")
         serverEnv.add("DOCKER_FILE=" + "${this.deploy_script_dir}/${this.subDir}" + "/docker_build/")
         this.script.echo "BuildImageStage serverEnv:${serverEnv}"
         this.script.withEnv(serverEnv) {
             this.script.sh "${this.deploy_script_dir}/${this.subDir}/build_image.sh"
         }
      }
    }

    def deleteLocalImages(){
        this.script.echo "删除本地镜像"
        // this.script.sh "docker rmi 192.168.100.36:1179/xiaoke/${imageName}-server:${version}"
        // this.script.sh "docker rmi  registry.cn-beijing.aliyuncs.com/kkb-xiaoke/${imageName}-server:${version}"
        // TODO mv script below to other place
        this.script.sh """
        for image in `docker image ls|grep ${this.imageName}|grep -v base|grep -v none|grep -E 'years ago|months ago|weeks ago'|awk '{print \$3}'`;
        do
            docker rmi -f \$image;
        done;
        for image_day in `docker image ls|grep ${this.imageName}|grep -v base|grep -v none|grep 'hours ago'|awk '{print \$3"==="\$4}'`;
        do 
            image=`echo \$image_day |sed 's/===.*//g'`;
            day=`echo \$image_day|sed 's/.*===//g'`;
            if [ \$day -gt 10 ];
            then 
                docker rmi -f \$image;
            fi;
        done
        """
    }

    def mvnPackage(){
        this.script.sh "cd ${this.script.env.WORKSPACE}"
        if ((this.subDir == ""||this.subDir == null)&& !this.newArch && this.config.jdkVersion) {
            this.script.sh "source /data/app/jdk15.sh && pwd && /usr/local/maven/bin/mvn -v && /usr/local/maven/bin/mvn clean -U package  -am -Dmaven.test.skip"
            this.script.sh "mv ./target/${this.imageName}.jar ${this.deploy_script_dir}/docker_build/app.jar"
            this.script.sh "ls ./target/ && ls ${this.deploy_script_dir} && ls ${this.deploy_script_dir}/docker_build/"
            this.subDir=""

        } else if ((this.subDir == ""||this.subDir == null)&& !this.newArch) {
          this.script.sh "pwd && /usr/local/maven/bin/mvn clean -U package  -am -Dmaven.test.skip"
          this.script.sh "mv ./target/${this.imageName}*.jar ${this.deploy_script_dir}/docker_build/app.jar"
          this.script.sh "ls ./target/ && ls ${this.deploy_script_dir} && ls ${this.deploy_script_dir}/docker_build/"
          this.subDir=""
        }
        else if (this.subDir && this.newArch){
          // version = this.script.sh(returnStdout: true, script: "cd ${subDir} && /usr/local/maven/bin/mvn -gs /root/.m2_arch/settings.xml -q   -Dexec.executable=\"echo\"   -Dexec.args='\${project.version}'   --non-recursive   org.codehaus.mojo:exec-maven-plugin:1.6.0:exec|tr -d '\n'")
          this.version = this.script.sh(returnStdout: true, script: "/usr/local/maven/bin/mvn  -q   -Dexec.executable=\"echo\"   -Dexec.args='\${project.version}'   --non-recursive   org.codehaus.mojo:exec-maven-plugin:1.6.0:exec|tr -d '\n'")
          this.config.version = this.config.version.replace('1.0.0', this.version)
          this.version = this.config.version
          // imageName = subDir
          // FIXME: mv subDir.jar app.jar to keep the image name
          // this.script.sh "pwd && /usr/local/maven/bin/mvn -gs /root/.m2_arch/settings.xml  clean package -pl ${subDir} -am -Dmaven.test.skip"

          this.script.sh """
          if [ -f ./${this.subDir}/src/main/resources/bootstrap-dev.yaml ];then
            cp ./${this.subDir}/src/main/resources/bootstrap-dev.yaml ./${this.subDir}/src/main/resources/bootstrap-kmos_dev.yaml
          fi
          """



          this.script.sh "pwd && /usr/local/maven/bin/mvn clean -U package -pl ${this.subDir} -am -Dmaven.test.skip"
          this.script.sh "mv ./${this.subDir}/target/${this.imageName}.jar ${this.deploy_script_dir}/${this.subDir}/docker_build/app.jar"
          // this.subDir=""
        } else if (this.newArch){
          // version = this.script.sh(returnStdout: true, script: "/usr/local/maven/bin/mvn -gs /root/.m2_arch/settings.xml -q   -Dexec.executable=\"echo\"   -Dexec.args='\${project.version}'   --non-recursive   org.codehaus.mojo:exec-maven-plugin:1.6.0:exec|tr -d '\n'")
          this.version = this.script.sh(returnStdout: true, script: "/usr/local/maven/bin/mvn  -q   -Dexec.executable=\"echo\"   -Dexec.args='\${project.version}'   --non-recursive   org.codehaus.mojo:exec-maven-plugin:1.6.0:exec|tr -d '\n'")
          this.config.version = this.config.version.replace('1.0.0', this.version)
          this.version = this.config.version
          this.subDir=""
          this.script.sh """
          if [ -f ./${this.subDir}/src/main/resources/bootstrap-dev.yaml ];then
            cp ./${this.subDir}/src/main/resources/bootstrap-dev.yaml ./${this.subDir}/src/main/resources/bootstrap-kmos_dev.yaml
          fi
          """
          this.script.sh "pwd && /usr/local/maven/bin/mvn clean  -U package  -am -Dmaven.test.skip"
          // this.script.sh "pwd && /usr/local/maven/bin/mvn -gs /root/.m2_arch/settings.xml  clean package  -am -Dmaven.test.skip"
          this.script.sh "mv ./target/${this.imageName}.jar ${this.deploy_script_dir}/docker_build/app.jar"
        } else{
          this.script.sh "pwd && /usr/local/maven/bin/mvn clean  -U package -pl ${this.subDir} -am -Dmaven.test.skip"
          this.script.sh "mv ./${this.subDir}/target/*.jar ${this.deploy_script_dir}/${this.subDir}/docker_build/app.jar"
          this.script.sh "ls ./${this.subDir}/target/ && ls ${this.deploy_script_dir}/${this.subDir} && ls ${this.deploy_script_dir}/${this.subDir}/docker_build/"
        }
    }

    def run() {
        this.imageName = this.config.name
        if (this.imageName=='kkb-bi-api'){
          this.imageName="bi-api"
        }
        if (this.imageName=='kkb-normmonitor'){
          this.imageName="norm-monitor"
        }

        this.subDir = this.config.subdir
        this.version = this.config.version
        this.newArch = this.config.newArch
        this.script.echo "config:${this.config}"
        this.script.currentBuild.description="${this.script.params.CHANGE_TYPE}:${this.script.env.gitlabSourceBranch}"
        this.script.node('jenkins-slave-k8s') {
            def deployEnv = this.deployEnv
            if (deployEnv=="test"||deployEnv=="test2"||deployEnv=="dev"){
                this.registryUrl = "192.168.1.8"
                this.registryLogIn = "fb4b2752-1983-4729-a992-5b22b7b9241c"
                this.registryNS = "xiaoke"
            } else if(deployEnv=="prod"||deployEnv=="pre"){
                this.registryUrl = "registry.cn-beijing.aliyuncs.com"
                this.registryLogIn = "kkb_aliyun_docker_registry_login"
                this.registryNS = "wuchenyang"
            }
            // def imageExist = this.script.sh(returnStdout: true, script: "docker images | grep  ${this.imageName}: | grep ${this.version} | tr -d '\n'")
            // if (imageExist!=""){
            //     def imageExistTag = this.script.sh(returnStdout: true, script: "docker images | grep  ${this.imageName}: | grep ${this.version} |head -1|awk '{print \$1\":\"\$2}'|tr -d '\n'")
            //     def fixedServerName = imageExistTag.replaceAll(".*/","").replaceAll(":.*","" )
            //     this.script.sh "docker tag ${imageExistTag} ${this.registryUrl}/${this.registryNS}/${fixedServerName}:${this.version}"
            //     this.script.sh "docker push  ${this.registryUrl}/${this.registryNS}/${fixedServerName}:${this.version}"
            //     this.script.echo "The image is exists, skip build"
            //     this.script.checkout this.script.scm
            //     pullScript()
            // }else{
                this.script.checkout this.script.scm
                this.deploy_script_dir = "cloud-config-mos"
                pullScript()
                this.config.gitCommitId = this.script.sh(returnStdout: true, script: "git rev-parse HEAD")
                this.mvnPackage()
                this.buildImage()
            // }
            this.deleteLocalImages()
            // 打jar包
            // wtf !!!
            // it is a mistake
            // this.script.sh "cd ${this.script.env.WORKSPACE}"
            // if ((subDir == ""||subDir == null)&& !newArch) {
            //   this.script.sh "pwd && /usr/local/maven/bin/mvn clean -U package  -am -Dmaven.test.skip"
            //   this.script.sh "mv ./target/${imageName}.jar ${this.deploy_script_dir}/docker_build/app.jar"
            //   this.script.sh "ls ./target/ && ls ${this.deploy_script_dir} && ls ${this.deploy_script_dir}/docker_build/"
            //   subDir=""
            // } else if (subDir && newArch){
            //   // version = this.script.sh(returnStdout: true, script: "cd ${subDir} && /usr/local/maven/bin/mvn -gs /root/.m2_arch/settings.xml -q   -Dexec.executable=\"echo\"   -Dexec.args='\${project.version}'   --non-recursive   org.codehaus.mojo:exec-maven-plugin:1.6.0:exec|tr -d '\n'")
            //   version = this.script.sh(returnStdout: true, script: "cd ${subDir} && /usr/local/maven/bin/mvn  -q   -Dexec.executable=\"echo\"   -Dexec.args='\${project.version}'   --non-recursive   org.codehaus.mojo:exec-maven-plugin:1.6.0:exec|tr -d '\n'")
            //   this.config.version = version
            //   // imageName = subDir
            //   // FIXME: mv subDir.jar app.jar to keep the image name
            //   // this.script.sh "pwd && /usr/local/maven/bin/mvn -gs /root/.m2_arch/settings.xml  clean package -pl ${subDir} -am -Dmaven.test.skip"
            //   this.script.sh "cp ${subDir}/src/main/resources/bootstrap-dev.yaml ${subDir}/src/main/resources/bootstrap-kmos_dev.yaml"
            //   this.script.sh "pwd && /usr/local/maven/bin/mvn clean -U package -pl ${subDir} -am -Dmaven.test.skip"
            //   this.script.sh "mv ./${subDir}/target/${subDir}.jar ${this.deploy_script_dir}/docker_build/app.jar"
            //   subDir=""
            // } else if (newArch){
            //   // version = this.script.sh(returnStdout: true, script: "/usr/local/maven/bin/mvn -gs /root/.m2_arch/settings.xml -q   -Dexec.executable=\"echo\"   -Dexec.args='\${project.version}'   --non-recursive   org.codehaus.mojo:exec-maven-plugin:1.6.0:exec|tr -d '\n'")
            //   version = this.script.sh(returnStdout: true, script: "/usr/local/maven/bin/mvn  -q   -Dexec.executable=\"echo\"   -Dexec.args='\${project.version}'   --non-recursive   org.codehaus.mojo:exec-maven-plugin:1.6.0:exec|tr -d '\n'")
            //   this.config.version = version
            //   subDir=""
            //   this.script.sh "cp src/main/resources/bootstrap-dev.yaml src/main/resources/bootstrap-kmos_dev.yaml"
            //   this.script.sh "pwd && /usr/local/maven/bin/mvn clean  -U package  -am -Dmaven.test.skip"
            //   // this.script.sh "pwd && /usr/local/maven/bin/mvn -gs /root/.m2_arch/settings.xml  clean package  -am -Dmaven.test.skip"
            //   this.script.sh "mv ./target/${imageName}.jar ${this.deploy_script_dir}/docker_build/app.jar"
            // } else{
            //   this.script.sh "pwd && /usr/local/maven/bin/mvn clean  -U package -pl ${subDir} -am -Dmaven.test.skip"
            //   this.script.sh "mv ./${subDir}/target/*.jar ${this.deploy_script_dir}/${subDir}/docker_build/app.jar"
            //   this.script.sh "ls ./${subDir}/target/ && ls ${this.deploy_script_dir}/${subDir} && ls ${this.deploy_script_dir}/${subDir}/docker_build/"
            // }
            // 构建QA环境镜像
            // this.script.docker.withRegistry('http://192.168.100.36:1179', 'kkb_docker_registry_login') {
            //     def serverEnv = []
            //     serverEnv.add("DOCKER_REGISTRY_URL=192.168.100.36:1179")
            //     serverEnv.add("DOCKER_NAME_SPACE=xiaoke")
            //     serverEnv.add("DOCKER_imageName=" + "${imageName}")
            //     serverEnv.add("DOCKER_TAG=" + "${version}")
            //     serverEnv.add("DOCKER_FILE=" + "${this.deploy_script_dir}/${subDir}" + "/docker_build/")
            // 
            //     this.script.echo "QABuildImageStage serverEnv:${serverEnv}"
            // 
            //     this.script.withEnv(serverEnv) {
            // 
            //         this.script.sh "${this.deploy_script_dir}/${subDir}/build_image.sh"
            //     }
            // }
            // 
            // // 构建pre、prod环境镜像
            // this.script.docker.withRegistry('http://registry.cn-beijing.aliyuncs.com', 'kkb_aliyun_docker_registry_login') {
            //     def preProEnv = []
            //     preProEnv.add("DOCKER_REGISTRY_URL=registry.cn-beijing.aliyuncs.com")
            //     preProEnv.add("DOCKER_NAME_SPACE=kaikeba")
            //     preProEnv.add("DOCKER_imageName=" + "${imageName}")
            //     preProEnv.add("DOCKER_TAG=" + "${version}")
            //     preProEnv.add("DOCKER_FILE=" + "${this.deploy_script_dir}/${subDir}" + "/docker_build/")
            // 
            //     this.script.echo "preProdBuildImageStage preProEnv:${preProEnv}"
            // 
            //     this.script.withEnv(preProEnv) {
            // 
            //         this.script.sh "${this.deploy_script_dir}/${subDir}/build_image.sh"
            //     }
            // }
        } 
    }

}

