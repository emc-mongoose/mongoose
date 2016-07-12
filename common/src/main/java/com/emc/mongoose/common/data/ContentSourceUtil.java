package com.emc.mongoose.common.data;

/**
 Created by kurila on 31.05.16.
 */
public class ContentSourceUtil {

	public static ContentSource clone(ContentSource anotherContentSrc) {
		if(anotherContentSrc instanceof FileContentSource) {
			return new FileContentSource((FileContentSource) anotherContentSrc);
		} else if(anotherContentSrc instanceof SeedContentSource) {
			return new SeedContentSource((SeedContentSource) anotherContentSrc);
		} else {
			throw new IllegalStateException("Unhandled content source type");
		}
	}
}
