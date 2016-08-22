package org.kisst.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TraceLog implements TraceItem {
	private static Logger LOG = LoggerFactory.getLogger(TraceLog.class);

	private final List<TraceItem> items=new ArrayList<>();
	private final boolean infoLog;

	public TraceLog(boolean infoLog) {
		this.infoLog = infoLog;
	}

	public void info(TraceItem i) {
		if (infoLog)
			LOG.info(i.toString());
		items.add(i);
	}
	public List<TraceItem> getItems() { return new ArrayList<>(items);}

	@Override public String getMessage() {
		StringBuilder result=new StringBuilder();
		for (TraceItem i: items)
			result.append(i.getMessage()+"\n");
		return result.toString();
	}
	@Override public String toString() { return getMessage();}
}
