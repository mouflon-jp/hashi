package com.github.mouflon_jp.jfiap.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.mail.Header;
import javax.xml.bind.JAXB;

import org.fiap.soap.DataRQ;
import org.fiap.soap.DataRS;
import org.fiap.soap.QueryRQ;
import org.fiap.soap.QueryRS;

import jp.gutp.fiap._2009._11.Body;
import jp.gutp.fiap._2009._11.Error;
import jp.gutp.fiap._2009._11.Key;
import jp.gutp.fiap._2009._11.OK;
import jp.gutp.fiap._2009._11.Point;
import jp.gutp.fiap._2009._11.PointSet;
import jp.gutp.fiap._2009._11.Query;
import jp.gutp.fiap._2009._11.Transport;
import jp.gutp.fiap._2009._11.Value;

public class FIAPObjects {

	private FIAPObjects(){};

	protected static String toXMLStringFromJAXBObject(Object jaxbObject) {
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)){
			JAXB.marshal(jaxbObject, pw);
		}
		return sw.toString();
	}

	public static Map<String, Map<Instant, String>> createPointMap(Iterable<Point> points){
		Map<String, Map<Instant, String>> ret = new HashMap<>();
		for (Point point : points) {
			if(!ret.containsKey(point.getId())){
				ret.put(point.getId(), new HashMap<>());
			}

			Map<Instant, String> values = ret.get(point.getId());
			values.putAll(
				point.getValue().stream()
				.collect(Collectors.toMap(
						it -> it.getTime().toGregorianCalendar().toInstant(),
						it -> it.getValue()
				))
			);
		};
		return ret;
	}

	public static List<Point> createPointList(Map<String, Map<Instant, String>> pointValues){
		List<Point> list = new ArrayList<>();
		GregorianCalendar cal = new GregorianCalendar();

		for (Entry<String, Map<Instant, String>> pointValue : pointValues.entrySet()) {
			Point point = new Point();
			point.setId(pointValue.getKey());

			point.getValue().addAll(
				pointValue.getValue().entrySet().stream()
				.map(it -> {
					Value value = new Value();
					cal.setTimeInMillis(it.getKey().toEpochMilli());
					value.setTime(DateTimeUtil.newXMLGregorianCalendar(cal));
					value.setValue(it.getValue());
					return value;
				})
				.collect(Collectors.toList())
			);

			list.add(point);
		}

		return list;
	}

	public static String toXMLString(DataRQ obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(DataRS obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(QueryRQ obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(QueryRS obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(Transport obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(Header obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(OK obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(Error obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(Query obj){ return toXMLStringFromJAXBObject(obj);	 }
	public static String toXMLString(Key obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(Body obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(Point obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(PointSet obj){ return toXMLStringFromJAXBObject(obj); }
	public static String toXMLString(Value obj){ return toXMLStringFromJAXBObject(obj); }

	public static String toXMLStringFromPoints(Iterable<Point> obj){
		Points p = new Points();
		p.point = StreamSupport.stream(obj.spliterator(), false)
				   .collect(Collectors.toList());
		return toXMLStringFromJAXBObject(p);
	}

	public static String toXMLStringFromPointSets(Iterable<PointSet> obj){
		PointSets ps = new PointSets();
		ps.pointSet = StreamSupport.stream(obj.spliterator(), false)
				       .collect(Collectors.toList());
		return toXMLStringFromJAXBObject(ps);
	}

	public static String toXMLStringFromValues(Iterable<Value> obj){
		Values v = new Values();
		v.values = StreamSupport.stream(obj.spliterator(), false)
				   .collect(Collectors.toList());
		return toXMLStringFromJAXBObject(v);
	}

	public static String toString(Iterable<Point> obj){
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (Point point : obj) {
			for(Value value : point.getValue()){
				sb.append("{")
				.append("\"id\":\"").append(point.getId()).append("\",")
				.append("\"time\":\"").append(value.getTime()).append("\",")
				.append("\"value\":\"").append(value.getValue()).append("\"")
				.append("},\n");
			}
		}
		sb.delete(sb.length() - ",\n".length(), sb.length());
		sb.append("]");
		return sb.toString();
	}

	public static String toString(Map<String, Map<Instant, String>> pointValues){
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (String id : pointValues.keySet()) {
			for(Entry<Instant, String> entry : pointValues.get(id).entrySet()){
				sb.append("{")
				.append("\"id\":\"").append(id).append("\",")
				.append("\"time\":\"").append(entry.getKey()).append("\",")
				.append("\"value\":\"").append(entry.getValue()).append("\"")
				.append("},\n");
			}
		}
		sb.delete(sb.length() - ",\n".length(), sb.length());
		sb.append("]");
		return sb.toString();
	}

	private static class Points {
		@SuppressWarnings("unused")
		public List<Point> point;
	}

	private static class PointSets {
		@SuppressWarnings("unused")
		public List<PointSet> pointSet;
	}

	private static class Values {
		@SuppressWarnings("unused")
		public List<Value> values;
	}
}
