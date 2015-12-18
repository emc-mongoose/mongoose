package com.emc.mongoose.common.log.appenders.processors;

import java.util.PriorityQueue;

public class VSimplifier {

	private WeighedLine line;
	private PriorityQueue<WeighedPoint> queue;

	public VSimplifier(Point ... points) {
		line = new WeighedLine(points);
		queue = new PriorityQueue<>();
		queue.addAll(line.points);
	}

	public void simplify(int simplificationsNum) {
		for (int i = 0; i < simplificationsNum; i++) {
			System.out.println(line); // for debug
			line.removeAndWeigh(queue.poll());
		}
		System.out.println(line); //for debug

	}

}
