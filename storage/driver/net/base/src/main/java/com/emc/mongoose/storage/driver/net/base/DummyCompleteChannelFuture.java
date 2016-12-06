package com.emc.mongoose.storage.driver.net.base;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.CompleteFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
/**
 Created by andrey on 06.12.16.
 */
public final class DummyCompleteChannelFuture
extends CompleteFuture<Void>
implements ChannelFuture {

	public DummyCompleteChannelFuture() {
		super(null);
	}

	@Override
	public final Channel channel() {
		return null;
	}

	@Override
	public final boolean isVoid() {
		return false;
	}

	@Override
	public final boolean isSuccess() {
		return false;
	}

	@Override
	public final Throwable cause() {
		return null;
	}

	@Override
	public final Void getNow() {
		return null;
	}

	@Override
	public final ChannelFuture sync()
	throws InterruptedException {
		return this;
	}

	@Override
	public final ChannelFuture syncUninterruptibly() {
		return this;
	}

	@Override
	public final ChannelFuture addListener(
		final GenericFutureListener<? extends Future<? super Void>> listener
	) {
		super.addListener(listener);
		return this;
	}

	@Override
	public final ChannelFuture addListeners(
		final GenericFutureListener<? extends Future<? super Void>>... listeners
	) {
		super.addListeners(listeners);
		return this;
	}

	@Override
	public final ChannelFuture removeListener(
		final GenericFutureListener<? extends Future<? super Void>> listener
	) {
		super.removeListener(listener);
		return this;
	}

	@Override
	public final ChannelFuture removeListeners(
		final GenericFutureListener<? extends Future<? super Void>>... listeners
	) {
		super.removeListeners(listeners);
		return this;
	}

	@Override
	public final ChannelFuture await()
	throws InterruptedException {
		return this;
	}

	@Override
	public final ChannelFuture awaitUninterruptibly() {
		return this;
	}

}
