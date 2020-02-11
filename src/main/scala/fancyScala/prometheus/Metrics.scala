package fancyScala.prometheus

import java.util.concurrent.Callable

import io.prometheus.client.exporter.PushGateway
import io.prometheus.client.{Collector, CollectorRegistry, Gauge}

class Metrics(val registry: CollectorRegistry,
              val getMetric: (String, String) => Gauge.Child,
              val pushToPrometheus: () => Unit) {

	protected def withHelp(help: String, default: String): String = if (help.isEmpty) default else help

	def setApplicationDuration(executionStart: Long, executionEnd: Long = System.currentTimeMillis()): Unit =
		set(name = "application_execution_duration_seconds", help = "Duration of the batch job in seconds.") {
			(executionEnd - executionStart) / Collector.MILLISECONDS_PER_SECOND
		}

	def startDurationTimer(name: String, help: String = ""): Gauge.Timer =
		startTimer(name + "_duration_seconds", withHelp(help, s"Duration of $name in seconds."))

	def setLastExecutionTime(name: String = "application", help: String = ""): Unit =
		setToCurrentTime(name + "_last_execution", withHelp(help, s"Last time $name was executed, in unixtime."))

	def setLastSuccessTime(name: String = "application", help: String = ""): Unit =
		setToCurrentTime(name + "_last_success", withHelp(help, s"Last time $name succeeded, in unixtime."))

	/**
	  * Set the gauge to the given value.
	  */
	def set(name: String, value: Double, help: String = ""): Unit = getMetric(name, help).set(value)

	/**
	  * Set the gauge to the given value. With a convenient syntax which is:
	  *
	  * val metrics = Metrics("namespace", "job", "127.0.0.1")
	  * metrics.set("very_metric_name", "useful help string") {
	  * randomInteger().toDouble / 1000
	  * }
	  */
	def set(name: String, help: String): Double => Unit = set(name, _, help)

	/**
	  * Set the gauge to the current unixtime.
	  */
	def setToCurrentTime(name: String, help: String = ""): Unit = getMetric(name, help).setToCurrentTime()

	/**
	  * Start a timer to track a duration.
	  * <p>
	  * Call {@link Timer#setDuration} at the end of what you want to measure the duration of.
	  * <p>
	  * This is primarily useful for tracking the durations of major steps of batch jobs,
	  * which are then pushed to a PushGateway.
	  * For tracking other durations/latencies you should usually use a {@link Summary}.
	  */
	def startTimer(name: String, help: String = ""): Gauge.Timer = getMetric(name, help).startTimer()

	/**
	  * Executes runnable code (e.g. a Java 8 Lambda) and observes a duration of how long it took to run.
	  *
	  * @return Measured duration in seconds for timeable to complete.
	  */
	def setToTime(name: String, timeable: Runnable, help: String): Double = getMetric(name, help).setToTime(timeable)

	def setToTime(name: String, timeable: Runnable): Double = setToTime(name, timeable, "")

	def setToTime(name: String, help: String): Runnable => Double = setToTime(name, _, help)

	def setToTime(name: String): Runnable => Double = setToTime(name, _)


}

object Metrics {

	def apply(namespace: String, job: String, address: String): Metrics =
		Metrics(namespace, subsystem = "", job, address, labels = Map.empty)

	def apply(namespace: String, subsystem: String, job: String, address: String): Metrics =
		Metrics(namespace, subsystem, job, address, labels = Map.empty)

	def apply(namespace: String, job: String, address: String, labels: Map[String, String]): Metrics =
		Metrics(namespace, subsystem = "", job, address, labels)

	def apply(namespace: String, subsystem: String, job: String, address: String, labels: Map[String, String]): Metrics = {
		val registry = new CollectorRegistry()
		val gaugeBuilder = createBuilder(namespace, subsystem, labels.keys.toSeq)
		val metricFunc = getMetric(registry, gaugeBuilder, labels.values.toSeq)
		val pushGateway = new PushGateway(address)
		val pushFunc: () => Unit = () => pushGateway.pushAdd(registry, job)

		new Metrics(registry, metricFunc, pushFunc)
	}

	/**
	  * create a Gauge Builder without labels
	  */
	protected def createBuilder(namespace: String, subsystem: String, labelNames: Seq[String]): Gauge.Builder = Gauge.build
		.namespace(Collector.sanitizeMetricName(namespace))
		.subsystem(Collector.sanitizeMetricName(subsystem))
		.labelNames(labelNames: _*)

	/**
	  * register a Collector
	  */
	protected def getMetric(registry: CollectorRegistry, gaugeBuilder: Gauge.Builder, labelValues: Seq[String]): (String, String) => Gauge.Child =
		(name: String, help: String) => gaugeBuilder.name(Collector.sanitizeMetricName(name)).help(help).register(registry).labels(labelValues: _*)
}