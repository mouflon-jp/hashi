package com.github.mouflon_jp.jfiap.ieee1888.object;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.mouflon_jp.jfiap.util.FIAPObjects;

import jp.gutp.fiap._2009._11.Body;
import jp.gutp.fiap._2009._11.Point;
import jp.gutp.fiap._2009._11.PointSet;

public class FIAPObjectBuilder extends Body {

	public static void main(String[] args) {

		Body body =
			new FIAPBodyBuilder()
			.pointSet("http://localhost/")
				.point("http://localhost/power")
					.data(Instant.now(), "test")
					.data(Instant.now().plusSeconds(10), "test")
					.data(Instant.now().plusSeconds(20), "test")
				.endPoint()
				.pointSet("http://localhost/hoge")
					.pointSet("http://localhost/hoge/foo")
						.point("http://localhost/fuga/foo/temp")
							.data(Instant.now(), "test")
							.data(Instant.now().plusSeconds(10), "test")
						.endPoint()
					.endPointSet()
					.pointSet("http://localhost/hoge/foo").endPointSet()
					.pointSet("http://localhost/fuga").endPointSet()
					.point("http://localhost/fuga/temp")
						.data(Instant.now(), "test")
						.data(Instant.now().plusSeconds(10), "test")
					.endPoint()
					.point("http://localhost/fuga/temp")
						.data(Instant.now(), "test")
						.data(Instant.now().plusSeconds(10), "test")
					.endPoint()
				.endPointSet()
			.endPointSet()
			.build();

		System.out.println(FIAPObjects.toXMLString(body));
	}


	public interface ObjectInterface {
		@Override
		String toString();
	}

	public static abstract class FIAPDataTreeInterface {
		protected String id;
		protected FIAPPointSetBuilder parent;

		public String getId(){
			return id;
		}

		public FIAPPointSetBuilder getParent(){
			return parent;
		}

		public abstract Body build();
	}

	public static class FIAPBodyBuilder extends FIAPPointSetBuilder {
		public FIAPBodyBuilder() {
			super("", null);
		}

		@Override
		public FIAPPointSetBuilder getParent() {
			StackTraceElement[] st = Thread.currentThread().getStackTrace();
			assert(st.length >= 2);
			if(st[1].getClassName().equals(FIAPBodyBuilder.class.getName())){
				return null;
			}

			return this;
		}

		@Override
		public Body build() {
			PointSet tmpps = generatePointSet();
			Body body = new Body();
			body.getPointSet().addAll(tmpps.getPointSet());
			body.getPoint().addAll(tmpps.getPoint());
			return body;
		}
	}


	public static class FIAPPointSetBuilder extends FIAPDataTreeInterface {
		private List<FIAPPointSetBuilder> pointsetList = new ArrayList<>();
		private List<FIAPPointBuilder>    pointList    = new ArrayList<>();

		public FIAPPointSetBuilder(String id, FIAPPointSetBuilder parent) {
			this.id = id;
			this.parent = parent;
		}

		public FIAPPointSetBuilder pointSet(String id){
			FIAPPointSetBuilder ps = new FIAPPointSetBuilder(id, this);
			pointsetList.add(ps);
			return ps;
		}

		public FIAPPointBuilder point(String id){
			FIAPPointBuilder ps = new FIAPPointBuilder(id, this);
			pointList.add(ps);
			return ps;
		}

		public FIAPPointSetBuilder endPointSet(){
			return getParent();
		}

		@Override
		public Body build() {
			return getParent().build();
		}

		protected PointSet generatePointSet(){
			PointSet ps = new PointSet();
			ps.setId(getId());
			ps.getPoint().addAll(
				pointList.stream()
				.map(it -> it.generatePoint())
				.collect(Collectors.toList())
			);
			ps.getPointSet().addAll(
				pointsetList.stream()
				.map(it -> it.generatePointSet())
				.collect(Collectors.toList())
			);
			return ps;
		}
	}

	public static class FIAPPointBuilder extends FIAPDataTreeInterface {
		private Map<Instant, Object> valueMap = new HashMap<>();

		public FIAPPointBuilder(String id, FIAPPointSetBuilder parent) {
			this.id = id;
			this.parent = parent;
		}

		public FIAPPointBuilder data(Instant time, Object value){
			valueMap.put(time, value);
			return this;
		}

		public FIAPPointSetBuilder pointSet(String id){
			return getParent().pointSet(id);
		}

		public FIAPPointBuilder point(String id) {
			return getParent().point(id);
		}

		public FIAPPointSetBuilder endPoint(){
			return getParent();
		}

		public FIAPPointSetBuilder endPointSet(){
			return getParent().getParent();
		}

		@Override
		public Body build() {
			return getParent().build();
		}

		protected Point generatePoint(){
			Point point = new Point();
			point.setId(getId());
			point.getValue().addAll(
					valueMap.entrySet().stream()
					.map(it ->
						FIAPObjectStaticDSL.value(
								it.getKey(),
								it.getValue().toString()
						)
					)
					.collect(Collectors.toList())
			);
			return point;
		}
	}
}
