package fancyScala

import fancyScala.models.HarvesterPipeline
import fancyScala.prometheus.Metrics

object CombinedHarvester extends App {

	val thisClassName: String = this.getClass.getName.replace("$", "")
	val thisClassSimpleName: String = this.getClass.getSimpleName.replace("$", "")

	// start collecting metrics data
	val metrics = Metrics("namespace", "Harvester", "127.0.0.1:9091")

	try {
		/**
		  * calls harvest() on each pipeline in same order as given by arguments
		  */
		args.foreach(id => {
			println(s"harvesting $id")
			HarvesterPipeline.getPipeline(id).harvest()
		})
		metrics.setLastSuccessTime()
	} finally {
		// end spark session
		metrics.setApplicationDuration(this.executionStart)
		metrics.setLastExecutionTime()
		metrics.pushToPrometheus()
	}
}
