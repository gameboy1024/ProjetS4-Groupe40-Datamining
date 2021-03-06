package projets4.case1.bolt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import projets4.basic.Arima;
import projets4.basic.Counter;
import projets4.case1.data.VideoInfoVariant;
import projets4.utils.TupleHelpers;
import backtype.storm.Config;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class ArimaBolt extends BaseRichBolt {

	private static final long serialVersionUID = 632265333724492194L;
	private static final int DEFAULT_EMIT_FREQUENCY_IN_SECONDS = 5;
	private static final int DEFAULT_COUNTER_SIZE = 10;
	private static final int DEFAULT_PREDICTION_NUMBER = 1;
	private static final Logger LOG = Logger.getLogger(ArimaBolt.class);
	private OutputCollector collector;
	private final int emitFrequencyInSeconds;
	private final int counterSize;
	private final int numPrediction;
	private Counter counter;
	private Arima arima;
	private int distinguisher;

	public ArimaBolt() {
		this(DEFAULT_EMIT_FREQUENCY_IN_SECONDS, DEFAULT_COUNTER_SIZE,
				DEFAULT_PREDICTION_NUMBER);
	}

	public ArimaBolt(int emitFrequencyInSeconds, int counterSize,
			int numPrediction) {
		this.counterSize = counterSize;
		this.numPrediction = numPrediction;
		this.counter = new Counter(this.counterSize);
		this.emitFrequencyInSeconds = emitFrequencyInSeconds;
		this.arima = new Arima();
		this.distinguisher = Integer.MIN_VALUE;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void prepare(Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;

	}

	@Override
	public void execute(Tuple input) {
		if (TupleHelpers.isTickTuple(input)) {
			LOG.info("Received tick tuple, triggering emit of predictive counts");
			emit(counter.getVariantInfos());
		} else {
			process(input);
		}
	}

	private void process(Tuple input) {
		Object obj = input.getValue(0);
		VideoInfoVariant count = (VideoInfoVariant) input.getValue(1);
		LOG.info("Get " + count.getStreamCount() + " as input count");
		counter.addEntry(obj, count);
		collector.ack(input);
		//LOG.info("Get " + count.getStreamCount() + " as input count over");
	}

	private void emit(Map<Object, LinkedList<VideoInfoVariant>> variantInfos) {
		for (Object o : variantInfos.keySet()) {
			LinkedList<VideoInfoVariant> infos = variantInfos.get(o);
			long [] inputs = new long[counterSize];
			int i=0;
			for (Iterator<VideoInfoVariant> iterator = infos.iterator(); iterator.hasNext();) {
				VideoInfoVariant videoInfoVariant = (VideoInfoVariant) iterator
						.next();
				inputs[i++] = videoInfoVariant.getStreamCount();
			}
			double[] prediction = arima.calculate(inputs, numPrediction, distinguisher);
			if (distinguisher == Integer.MAX_VALUE) {
				distinguisher = Integer.MIN_VALUE;
			}
			
			LOG.info("Got prediction number :  " + prediction[0]);
			collector.emit(new Values(o,prediction));
		}
		
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("obj", "count_prediction"));
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		Map<String, Object> conf = new HashMap<String, Object>();
		conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, emitFrequencyInSeconds);
		return conf;
	}

}
