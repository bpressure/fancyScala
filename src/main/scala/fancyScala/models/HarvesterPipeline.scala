package fancyScala.models

import fancyScala.pipelines.DummyPipeline
import fancyScala.prometheus.Metrics
import io.prometheus.client.Gauge

trait HarvesterPipelineCompanion {
	val id: String
}

/**
  * each HarvesterPipeline should have a function to read its own Master collection
  * ES should be
  */
trait HarvesterPipeline {

	val id: String
	lazy val thisClassSimpleName: String = this.getClass.getSimpleName.replace("$", "")

	/**
	  * read the dailies table
	  *
	  * @return the dailies table
	  */
	final protected def readDailies: Seq[String] = Seq.empty[String]

	/**
	  * read the master table
	  *
	  * @return the master table
	  */
	final protected def readMaster: Seq[String] = Seq.empty[String]

	/**
	  * adds the daily aggregation to the dailies table
	  */
	final protected def saveDaily(data: Seq[String]): Unit = {}

	/**
	  * replaces the master aggregation in the master table
	  */
	final protected def saveMaster(data: Seq[String]): Unit = {}

	/**
	  * produce daily aggregate for the specified date
	  *
	  * @return aggregate of the given day
	  */
	protected def processDaily(): Seq[String]

	/**
	  * calculate data for the master table for the first time
	  *
	  * @param daily         the aggregate of the given day
	  * @return new updated master data
	  */
	protected def processMasterInitially(daily: Seq[String]): Seq[String]

	/**
	  * calculate data for the master table
	  * must be written in a way that keeps the _id field, so that saving later can update the data
	  *
	  * @param currentMaster    the current master data
	  * @param daily            the aggregate of the given day
	  * @return new updated master data
	  */
	protected def processMaster(currentMaster: Seq[String], daily: Seq[String]): Seq[String]

	/**
	  * recalculate the entire Master table from the daily aggregates (in case of catastrophes)
	  *
	  * @param dailies      all the daily aggregates
	  */
	protected def reprocessMaster(dailies: Seq[String]): Seq[String]

	private lazy val newDaily: Seq[String] = {
		println(s"Start processing daily in $thisClassSimpleName")
		processDaily()
	}

	/**
	  * wrapper to execute the entire pipeline ETL
	  */
	final def harvest(): Unit = measureJob("harvest") {
		// somehow make processMetrics:Metric implicitly available in here
		val dailiesTimer: Gauge.Timer = processMetrics.startTimer("process_daily", "time needed to create and store the daily aggregation")
		saveDaily(newDaily)
		dailiesTimer.setDuration()

		def newMaster = {
			println("Start processing master in " + thisClassSimpleName)
			val currentMaster = readMaster
			if (currentMaster.isEmpty) {
				val masterTimer: Gauge.Timer = processMetrics.startTimer("reprocess_master", "time needed to initialize the master aggregation")
				val result = processMasterInitially(newDaily)
				masterTimer.setDuration()
				result
			} else {
				val masterTimer: Gauge.Timer = processMetrics.startTimer("reprocess_master", "time needed to add the daily to the master aggregation")
				val result = processMaster(currentMaster, newDaily)
				masterTimer.setDuration()
				result
			}
		}

		saveMaster(newMaster)
	}

	/**
	  * allows us to re-processing dailies without re-processing the master
	  */
	final def reHarvestDaily(): Unit = measureJob("reHarvestDaily") {
		// somehow make processMetrics:Metric implicitly available in here
		val dailiesTimer: Gauge.Timer = processMetrics.startTimer("process_daily", "time needed to create and store the daily aggregation")
		saveDaily(newDaily)
		dailiesTimer.setDuration()
	}

	/**
	  * wrapper to execute the whole master re-processing
	  */
	final def reHarvestMaster(): Unit = measureJob("reHarvestMaster") {
		// somehow make processMetrics:Metric implicitly available in here

		val dailiesTimer: Gauge.Timer = processMetrics.startTimer("read_dailies", "time needed to digest the dailies table")
		val dailies = readDailies
		dailiesTimer.setDuration()

		val masterTimer: Gauge.Timer = processMetrics.startTimer("reprocess_master", "time needed to reprocess the master table")
		saveMaster(reprocessMaster(dailies))
		masterTimer.setDuration()
	}

	def measureJob(job: String)(func: => Unit): Unit = {
		implicit val processMetrics: Metrics = Metrics("namespace", thisClassSimpleName, "pipeline_execution", "127.0.0.1", Map("job" -> job, "pipeline_id" -> id))
		val processTimer = processMetrics.startDurationTimer("execution", "time needed to calculate the result of this test")
		try {
			func
			processMetrics.setLastSuccessTime()
		} finally {
			processTimer.setDuration()
			processMetrics.setLastExecutionTime()
			processMetrics.pushToPrometheus()
		}
	}
}

object HarvesterPipeline {

	def getPipeline(id: String): HarvesterPipeline =
		id.toLowerCase() match {
			case DummyPipeline.id => new DummyPipeline()
			case _ => throw new PipelineNotFoundException(s"could not find pipeline with id $id")
		}
}