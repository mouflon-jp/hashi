package com.github.mouflon_jp.jfiap.util;

import java.util.List;
import java.util.concurrent.Callable;

import javax.xml.datatype.XMLGregorianCalendar;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.FieldProxy;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;

public class FIAPObjectDebugUtil {
	private FIAPObjectDebugUtil(){};

	protected static void overrideToString(String className, Class<?> methodClass){
		TypePool typePool = TypePool.Default.ofClassPath();
		new ByteBuddy()
	      .redefine(
	    		  typePool.describe(className).resolve(),
	              ClassFileLocator.ForClassLoader.ofClassPath()
	      )
	      .method(ElementMatchers.named("toString"))
	      .intercept(
	    		  MethodDelegation.to(methodClass)
	    		  .appendParameterBinder(FieldProxy.Binder.install(FieldGetter.class, FieldSetter.class))
	      )
	      .make()
	      .load(ClassLoader.getSystemClassLoader());
	}

	public static void enableValueDebug(){
		overrideToString(ValueInterceptor.CLASS_NAME, ValueInterceptor.class);
	}

	public static void enablePointDebug(){
		overrideToString(PointInterceptor.CLASS_NAME, PointInterceptor.class);
	}

	public static void enablePointSetDebug(){
		overrideToString(PointSetInterceptor.CLASS_NAME, PointSetInterceptor.class);
	}

	public static void enableBodyDebug(){
		overrideToString(BodyInterceptor.CLASS_NAME, BodyInterceptor.class);
	}

	public static interface FieldGetter<T> {
		T getValue();
	}

	public static interface FieldSetter<T> {
		void setValue(T value);
	}

	public static class ValueInterceptor {
		public static final String CLASS_NAME = "jp.gutp.fiap._2009._11.Value";
		public static String toString(
				@SuperCall Callable<String> superMethod,
				@FieldProxy("time")  FieldGetter<XMLGregorianCalendar> timeGetter,
				@FieldProxy("value") FieldGetter<String> valueGetter
		) {
			return "Value:[time=" + timeGetter.getValue() + ", value=" + valueGetter.getValue() + "]";
		}
	}

	public static class PointInterceptor {
		public static final String CLASS_NAME = "jp.gutp.fiap._2009._11.Point";
		public static String toString(
				@SuperCall Callable<String> superMethod,
				@FieldProxy("id")    FieldGetter<String>  idGetter,
				@FieldProxy("value") FieldGetter<List<?>> valueGetter
		) {
			StringBuilder sb = new StringBuilder();
			sb.append("Point:[id=" + idGetter.getValue());

			sb.append(", value=");
			List<?> values = valueGetter.getValue();
			if(values != null){
				sb.append("{");
				for (int i=0; i<values.size(); i++) {
					if(i!=0){ sb.append(","); }
					sb.append(values.get(i).toString());
				}
				sb.append("}");
			} else {
				sb.append("null");
			}

			sb.append("]");
			return sb.toString();
		}
	}

	public static class PointSetInterceptor {
		public static final String CLASS_NAME = "jp.gutp.fiap._2009._11.PointSet";

		public static String toString(
				@SuperCall Callable<String> superMethod,
				@FieldProxy("id")       FieldGetter<String>  idGetter,
				@FieldProxy("point")    FieldGetter<List<?>> pointGetter,
				@FieldProxy("pointSet") FieldGetter<List<?>> pointSetGetter
		) {

			StringBuilder sb = new StringBuilder();
			sb.append("PointSet:[id=" + idGetter.getValue());

			sb.append(", point=");
			List<?> points = pointGetter.getValue();
			if(points != null){
				sb.append("{");
				for (int i=0; i<points.size(); i++) {
					if(i!=0){ sb.append(","); }
					sb.append(points.get(i).toString());
				}
				sb.append("}");
			} else {
				sb.append("null");
			}

			sb.append(", pointSet=");
			List<?> pointSets = pointSetGetter.getValue();
			if(pointSets != null){
				sb.append("{");
				for (int i=0; i<pointSets.size(); i++) {
					if(i!=0){ sb.append(","); }
					sb.append(pointSets.get(i).toString());
				}
				sb.append("}");
			} else {
				sb.append("null");
			}

			sb.append("]");
			return sb.toString();
		}
	}

	public static class BodyInterceptor {
		public static final String CLASS_NAME = "jp.gutp.fiap._2009._11.Body";

		public static String toString(
				@SuperCall Callable<String> superMethod,
				@FieldProxy("point")    FieldGetter<List<?>> pointGetter,
				@FieldProxy("pointSet") FieldGetter<List<?>> pointSetGetter
		) {

			StringBuilder sb = new StringBuilder();
			sb.append("Body:[");

			sb.append("point=");
			List<?> points = pointGetter.getValue();
			if(points != null){
				sb.append("{");
				for (int i=0; i<points.size(); i++) {
					if(i!=0){ sb.append(","); }
					sb.append(points.get(i).toString());
				}
				sb.append("}");
			} else {
				sb.append("null");
			}

			sb.append(", pointSet=");
			List<?> pointSets = pointSetGetter.getValue();
			if(pointSets != null){
				sb.append("{");
				for (int i=0; i<pointSets.size(); i++) {
					if(i!=0){ sb.append(","); }
					sb.append(pointSets.get(i).toString());
				}
				sb.append("}");
			} else {
				sb.append("null");
			}

			sb.append("]");
			return sb.toString();
		}
	}
}
