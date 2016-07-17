package com.github.mouflon_jp.jfiap.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class DateTimeUtil {
	private static final ThreadLocal<DatatypeFactory> dtfactory = new ThreadLocal<DatatypeFactory>(){
		@Override
		protected DatatypeFactory initialValue() {
			try {
				return DatatypeFactory.newInstance();
			} catch (DatatypeConfigurationException e) {
				throw new RuntimeException(e);
			}
		}
	};

	private DateTimeUtil(){};

	public static XMLGregorianCalendar newXMLGregorianCalendar(Date time) {
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(time);
		return newXMLGregorianCalendar(c);
	};

	public static XMLGregorianCalendar newXMLGregorianCalendar(Calendar time){
		XMLGregorianCalendar c;
		if(time instanceof GregorianCalendar){
			c = dtfactory.get().newXMLGregorianCalendar((GregorianCalendar)time);
		} else {
			c = dtfactory.get().newXMLGregorianCalendar(
					time.get(Calendar.YEAR),
					time.get(Calendar.MONTH) + 1,
					time.get(Calendar.DAY_OF_MONTH),
					time.get(Calendar.HOUR_OF_DAY),
					time.get(Calendar.MINUTE),
					time.get(Calendar.SECOND),
					time.get(Calendar.MILLISECOND),
					(int)TimeUnit.MILLISECONDS.toMinutes(
							time.get(Calendar.ZONE_OFFSET) - time.get(Calendar.DST_OFFSET)
					)
				);
		}
		return c;
	}

	public static XMLGregorianCalendar newXMLGregorianCalendar(Instant time) {
		GregorianCalendar c = new GregorianCalendar();
		c.setTimeInMillis(time.toEpochMilli());
		return newXMLGregorianCalendar(c);
	};

	public static XMLGregorianCalendar newXMLGregorianCalendar(LocalDateTime time) {
		return newXMLGregorianCalendar(ZonedDateTime.of(time, ZoneId.systemDefault()));
	};

	public static XMLGregorianCalendar newXMLGregorianCalendar(ZonedDateTime time) {
		return newXMLGregorianCalendar(time.toInstant());
	};
}
