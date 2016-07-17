package com.github.mouflon_jp.jfiap.ieee1888.object;

import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.datatype.XMLGregorianCalendar;

import com.github.mouflon_jp.jfiap.util.DateTimeUtil;
import com.github.mouflon_jp.jfiap.util.FIAPObjects;

import jp.gutp.fiap._2009._11.Body;
import jp.gutp.fiap._2009._11.Point;
import jp.gutp.fiap._2009._11.PointSet;
import jp.gutp.fiap._2009._11.Value;

public class FIAPObjectStaticDSL extends Body {

	public static void main(String[] args) {
		Body body = body(
			pointSet("http://localhost/",
				point("http://localhost/power",
					value(Instant.now(), "test"),
					value(Instant.now(), "test")
				),
				point("http://localhost/power",
					value(Instant.now(), "test"),
					value(Instant.now(), "test")
				)
			),
			point("http://localhost/power",
				value(Instant.now(), "test"),
				value(Instant.now(), "test")
			),
			point("http://localhost/power",
				value(Instant.now(), "test"),
				value(Instant.now(), "test")
			)
		);
		System.out.println(FIAPObjects.toXMLString(body));
	}

	private FIAPObjectStaticDSL(){};

	public static Body body(FIAPDataObjectInterface... name){
		Body body = new Body();
		FIAPDataObjectPointSet pset = pointSet("", name);
		body.getPoint().addAll(pset.getPoint());
		body.getPointSet().addAll(pset.getPointSet());

		return body;
	};

	public static FIAPDataObjectPointSet pointSet(String id, FIAPDataObjectInterface... name){
		FIAPDataObjectPointSet pset = new FIAPDataObjectPointSet();
		pset.setId(id);
		for (FIAPDataObjectInterface obj : name) {
			if(obj instanceof FIAPDataObjectPointSet){
				pset.getPointSet().add((FIAPDataObjectPointSet)obj);
			}
			if(obj instanceof FIAPDataObjectPoint){
				pset.getPoint().add((FIAPDataObjectPoint)obj);
			}
		}
		return pset;
	};

	public static FIAPDataObjectPoint point(String id, Value... data){
		FIAPDataObjectPoint point = new FIAPDataObjectPoint();
		point.setId(id);
		point.getValue().addAll(Stream.of(data).collect(Collectors.toList()));
		return point;
	};

	public static Value value(XMLGregorianCalendar time, Object value) {
		Value v = new Value();
		v.setTime(time);
		v.setValue(value.toString());
		return v;
	};

	public static Value value(Instant time, Object value) {
		return value(DateTimeUtil.newXMLGregorianCalendar(time), value);
	};

	public static interface FIAPDataObjectInterface {};
	public static class FIAPDataObjectPoint extends Point implements FIAPDataObjectInterface {};
	public static class FIAPDataObjectPointSet extends PointSet implements FIAPDataObjectInterface {};
}
