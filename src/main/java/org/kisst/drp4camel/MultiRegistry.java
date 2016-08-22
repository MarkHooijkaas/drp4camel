package org.kisst.drp4camel;

import org.apache.camel.NoSuchBeanException;
import org.apache.camel.spi.Registry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MultiRegistry  implements Registry {
	private final Registry[] registries;

	public MultiRegistry(Registry... registries) {this.registries=registries; }

	public Object lookupByName(String name) {
		Object result=null;
		for (Registry r: registries) {
			result=r.lookupByName(name);
			if (result!=null)
				return result;
		}
		return null;
	}


	public <T> T lookupByNameAndType(String name, Class<T> type) {
		T result=null;
		for (Registry r: registries) {
			result=r.lookupByNameAndType(name,type);
			if (result!=null)
				return result;
		}
		return null;
	}

	public <T> Map<String, T> findByTypeWithName(Class<T> type) {
		Map<String, T> result = new HashMap<String, T>();
		for (Registry r: registries) {
			Map<String, T> result2 = r.findByTypeWithName(type);
			if (result2!=null)
				result.putAll(result2);
		}
		return result;
	}

	public <T> Set<T> findByType(Class<T> type) {
		Set<T> result = new HashSet<T>();
		for (Registry r: registries) {
			Set<T> result2 = r.findByType(type);
			if (result2!=null)
				result.addAll(result2);
		}
		return result;
	}

	public Object lookup(String name) {
		return lookupByName(name);
	}

	public <T> T lookup(String name, Class<T> type) {
		return lookupByNameAndType(name, type);
	}

	public <T> Map<String, T> lookupByType(Class<T> type) {
		return findByTypeWithName(type);
	}
}
