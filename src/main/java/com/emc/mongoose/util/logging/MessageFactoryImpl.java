package com.emc.mongoose.util.logging;

import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ObjectMessage;

/**
 * Created by gusakk on 11/5/14.
 */
public class MessageFactoryImpl implements MessageFactory {

	private RunTimeConfig runTimeConfig;

	public MessageFactoryImpl(RunTimeConfig runTimeConfig) {
		this.runTimeConfig = runTimeConfig;
	}

	@Override
	public Message newMessage(Object message) {
		return new ObjectMessage(message);
	}

	@Override
	public Message newMessage(String message) {
		AdvancedParameterizedMessage parameterizedMessage = new AdvancedParameterizedMessage(message, null);
		if (runTimeConfig != null) {
			parameterizedMessage.put(Main.KEY_RUN_ID, runTimeConfig.getString(Main.KEY_RUN_ID));
		}
		return parameterizedMessage;
	}

	@Override
	public Message newMessage(String message, Object... params) {
		AdvancedParameterizedMessage parameterizedMessage = new AdvancedParameterizedMessage(message, params, null);
		if (runTimeConfig != null) {
			parameterizedMessage.put(Main.KEY_RUN_ID, runTimeConfig.getString(Main.KEY_RUN_ID));
		}
		return parameterizedMessage;
	}

}
