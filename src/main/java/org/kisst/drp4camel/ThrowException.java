package org.kisst.drp4camel;

import org.slf4j.helpers.MessageFormatter;

public class ThrowException {
	private final String format;

	public ThrowException(String format) {this.format=format;}
	public String it(Object ... obj ) { throw new RuntimeException(MessageFormatter.format(format,obj).getMessage()); }
	public String msg(String format, Object obj ) { throw new RuntimeException(MessageFormatter.format(format,obj).getMessage()); }
	public String msg(String format, Object obj1, Object obj2 ) { throw new RuntimeException(MessageFormatter.format(format,obj1, obj2).getMessage()); }

}
