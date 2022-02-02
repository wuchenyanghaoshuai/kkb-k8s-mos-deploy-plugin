package com.kaikeba.k8s.stages

import org.yubing.delivery.Stage

/**
 *	确认消息
 */
class ConfirmMessgerStage extends Stage {
	def tips
	def timeout
	def timeoutUnit

	def script
	def config

	ConfirmMessgerStage(project, stageName, tips) {
		super(project, stageName)

		this.script = project.script
		this.config = project.config
		
		this.tips = tips

		if (this.config.timeout == '' || this.config.timeout == null) {
        	this.timeout = 1
        } else {
        	this.timeout = this.config.timeout
        }

        if (this.config.timeoutUnit == '' ||this.config.timeoutUnit == null){
            this.timeoutUnit = 'HOURS'
        } else {
        	this.timeoutUnit = this.config.timeoutUnit
        }
	}

	def run() {
		this.script.timeout(time: this.timeout, unit: this.timeoutUnit){
			this.script.input message: this.tips
		}
	}
}