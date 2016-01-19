package com.emc.mongoose.common.log.appenders.processors;

import org.apache.logging.log4j.core.LogEvent;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class VSimplifierLogAdapter {

	private List<LogEvent> logEventList;

	VSimplifier durationMinSr, durationMaxSr, durationAvgSr,
				latencyMinSr, latencyMaxSr, latencyAvgSr,
				tpAvgSr, tpAvgLastSr,
				bwAvgSr, bwAvgLastSr;

	List<List<Point>> lists = new ArrayList<>();

	private final static int NUMBER_OF_METRICS = 4;

	List<Point> durationMinList, durationMaxList, durationAvgList,
			latencyMinList, latencyMaxList, latencyAvgList,
			tpAvgList, tpAvgLastList,
			bwAvgList, bwAvgLastList;

	public VSimplifierLogAdapter(LogEvent ... events) {
		logEventList = Arrays.asList(events);
		initiateLists();
		int counter = 0; //todo this is a temp decision
		for (LogEvent event: events) {
			String[] splitMsg = event.getMessage().toString().split(";");
			while (counter * NUMBER_OF_METRICS <= splitMsg.length) {
				addPointsToLists(counter, parseMsgValues(splitMsg[1]),
						durationMinList, durationMaxList, durationAvgList);
				addPointsToLists(counter, parseMsgValues(splitMsg[2]),
						latencyMinList, latencyMaxList, latencyAvgList);
				addPointsToLists(counter, parseMsgValues(splitMsg[3]),
						tpAvgList, tpAvgLastList);
				addPointsToLists(counter, parseMsgValues(splitMsg[4]),
						bwAvgList, bwAvgLastList);
				counter++;
			}
		}
	}

	public VSimplifierLogAdapter(List<LogEvent> events) {
		logEventList = events;
		initiateLists();
		int counter = 0; //todo this is a temp decision
		for (LogEvent event: events) {
			String[] splitMsg = event.getMessage().toString().split(";");
			while (counter * NUMBER_OF_METRICS <= splitMsg.length) {
				addPointsToLists(counter, parseMsgValues(splitMsg[1]),
						durationMinList, durationMaxList, durationAvgList);
				addPointsToLists(counter, parseMsgValues(splitMsg[2]),
						latencyMinList, latencyMaxList, latencyAvgList);
				addPointsToLists(counter, parseMsgValues(splitMsg[3]),
						tpAvgList, tpAvgLastList);
				addPointsToLists(counter, parseMsgValues(splitMsg[4]),
						bwAvgList, bwAvgLastList);
				counter++;
			}
		}
	}

	public List<LogEvent> simplify(int simplificationsNum) {
		return logEventList;
	}

	/***
	 *
	 * @param msgString has a kind of "duration[us]=(0/0/0)" format
	 */
	private String[] parseMsgValues(String msgString) {
		msgString = msgString.substring(msgString.indexOf("("))
				.replace("(", "").replace(")", "").replace("]", "");
		return msgString.split("/");
	}

	private void initiateLists() {
		for (int i = 0; i < 10; i++) {
			lists.add(new ArrayList<Point>());
		}
		durationMinList = lists.get(0);
		durationMaxList = lists.get(1);
		durationAvgList = lists.get(2);
		latencyMinList = lists.get(3);
		latencyMaxList = lists.get(4);
		latencyAvgList = lists.get(5);
		tpAvgList = lists.get(6);
		tpAvgLastList = lists.get(7);
		bwAvgList = lists.get(8);
		bwAvgLastList = lists.get(9);
	}

	private void addPointsToLists(int counter, String[] values,
	                              List<Point> list1, List<Point> list2) {
		list1.add(new Point(counter, Double.parseDouble(values[0])));
		list2.add(new Point(counter, Double.parseDouble(values[1])));
	}

	private void addPointsToLists(int counter, String[] values,
	                              List<Point> list1, List<Point> list2, List<Point> list3) {
		addPointsToLists(counter, values, list1, list2);
		list3.add(new Point(counter, Double.parseDouble(values[2])));
	}

}
