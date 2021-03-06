/**
 * Copyright (C) 2016 - 2030 youtongluan.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yx.db.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yx.bean.Bean;
import org.yx.db.annotation.CacheType;
import org.yx.db.event.QueryEvent;
import org.yx.db.sql.PojoMeta;
import org.yx.db.sql.PojoMetaHolder;
import org.yx.listener.SumkEvent;
import org.yx.log.Log;
import org.yx.redis.RecordReq;
import org.yx.util.GsonUtil;

@Bean
public class QueryListener implements DBListener<QueryEvent> {

	@Override
	public boolean accept(SumkEvent event) {
		return QueryEvent.class.isInstance(event);
	}

	@Override
	public void listen(QueryEvent event) {
		try {
			PojoMeta pm = PojoMetaHolder.getTableMeta(event.getTable());
			if (pm == null || pm.isNoCache() || event.getResult() == null) {
				return;
			}
			List<Map<String, Object>> in = event.getIn();
			if (in == null || in.size() != 1) {
				return;
			}

			Map<String, Object> where = in.get(0);
			if (!pm.isOnlyRedisID(where)) {
				return;
			}
			String id = pm.getRedisID(where, false);
			if (id == null) {
				return;
			}

			List<Object> list = new ArrayList<>(4);
			for (Object obj : event.getResult()) {
				if (id.equals(pm.getRedisID(obj, false))) {
					list.add(obj);
				}
			}
			if (list.isEmpty()) {
				return;
			}

			if (pm.cacheType() == CacheType.LIST) {
				RecordReq.set(pm, id, GsonUtil.toJson(list));
				return;
			}
			if (list.size() != 1 || list.get(0) == null) {
				return;
			}
			RecordReq.set(pm, id, GsonUtil.toJson(list.get(0)));
		} catch (Exception e) {
			Log.printStack("db-listener", e);
		}
	}

	@Override
	public String[] getTags() {
		return null;
	}

}
