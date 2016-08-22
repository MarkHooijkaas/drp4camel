package org.kisst.util;

import org.slf4j.helpers.MessageFormatter;

public interface TraceItem {
	public abstract String getMessage();

	public static abstract class Base implements TraceItem {
		@Override public String toString() { return this.getClass().getSimpleName()+"["+getMessage()+"]";}
		@Override public abstract String getMessage();
	}
	public static class MessageBase1<T> extends Base {
		private final String message;
		public final T obj1;
		public MessageBase1(String message, T obj1) {
			this.message = message;
			this.obj1=obj1;
		}
		@Override public String getMessage() { return MessageFormatter.format(message,obj1).getMessage(); }
	}
	public static class MessageBase2<T1, T2> extends Base {
		private final String message;
		public final T1 obj1;
		public final T2 obj2;
		public MessageBase2(String message, T1 obj1, T2 obj2) {
			this.message = message;
			this.obj1=obj1;
			this.obj2=obj2;
		}
		@Override public String getMessage() { return MessageFormatter.format(message,obj1, obj2).getMessage(); }
	}
}
