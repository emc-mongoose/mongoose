package com.emc.mongoose.core.impl.load.tasks.processors;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 Created on 25.07.16.
 */
public class PolylineTest {

	private static final int NUMBER_OF_NEW_POINTS = 100000;
	private static final int POLYLINE_POINTS_LIMIT = 50;
	private static final int SIMPLIFICATION_NUMBER = POLYLINE_POINTS_LIMIT / 2;
	private Polyline polyline;
	private Random random;

	@Before
	public void setUp() {
		polyline = new Polyline();
		random = new Random();
	}

	@Test
	public void shouldSimplify()
	throws Exception {
		for(int i = 0; i < NUMBER_OF_NEW_POINTS; i++) {
			if(polyline.numberOfPoints() == POLYLINE_POINTS_LIMIT) {
				polyline.simplify(SIMPLIFICATION_NUMBER);
			}
			polyline.addPoint(new Point(i, random.nextDouble() * random.nextInt(100)));
		}
	}
}