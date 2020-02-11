package fancyScala.pipelines

import fancyScala.models.{HarvesterPipeline, HarvesterPipelineCompanion}

/**
  * Convenience object to have access to static members
  */
object DummyPipeline extends DummyPipelineCompanion

/**
  * Here is where we define static members / constants so we can extends from this in both, the object and the class
  * and don't have to define stuff twice
  */
trait DummyPipelineCompanion extends HarvesterPipelineCompanion {
	val id: String = "world"
}

class DummyPipeline() extends HarvesterPipeline with DummyPipelineCompanion {

	// SOURCES
	lazy val logs: Seq[String] = Seq.empty
	lazy val tests: Seq[String] = Seq.empty

	/**
	  * produce daily aggregate for the specified date
	  *
	  * @return aggregate of the given day
	  */
	protected def processDaily(): Seq[String] = logs

	/**
	  * calculate data for the master table for the first time
	  *
	  * @param daily         the aggregate of the given day
	  * @return new updated master data
	  */
	protected def processMasterInitially(daily: Seq[String]): Seq[String] = logs

	/**
	  * calculate data for the master table
	  * must be written in a way that keeps the _id field, so that saving later can update the data
	  *
	  * @param currentMaster    the current master data
	  * @param daily            the aggregate of the given day
	  * @return new updated master data
	  */
	protected def processMaster(currentMaster: Seq[String], daily: Seq[String]): Seq[String] = logs

	/**
	  * recalculate the entire Master table from the daily aggregates (in case of catastrophes)
	  *
	  * @param dailies      all the daily aggregates
	  */
	protected def reprocessMaster(dailies: Seq[String]): Seq[String] = logs
}
