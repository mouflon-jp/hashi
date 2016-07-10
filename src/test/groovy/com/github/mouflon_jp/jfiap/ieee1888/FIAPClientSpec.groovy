package com.github.mouflon_jp.jfiap.ieee1888

import jp.gutp.fiap._2009._11.Transport

import org.fiap.soap.DataRQ
import org.fiap.soap.DataRS
import org.fiap.soap.QueryRQ
import org.fiap.soap.QueryRS

import spock.lang.Specification

class FIAPClientSpec extends Specification {
	static final TEST_STORAGE = 'http://fiap-sandbox.gutp.ic.i.u-tokyo.ac.jp/axis2/services/FIAPStorage?wsdl'

	def "queryメソッドにより通信できるか"(){
		setup:
			FIAPClient client = new FIAPClient(TEST_STORAGE)
			QueryRQ queryRQ = new QueryRQ()
			Transport transport = new Transport()
			queryRQ.transport = transport
			def ret

		when:
			ret = client.query(queryRQ)

		then:
			assert ret != null
			assert ret.class == QueryRS
			assert ret?.transport?.header?.error != null

		when:
			ret = client.query(transport)

		then:
			assert ret != null
			assert ret.class == Transport
			assert ret?.header?.error != null
	}

	def "dataメソッドにより通信できるか"(){
		setup:
			FIAPClient client = new FIAPClient(TEST_STORAGE)
			DataRQ dataRQ = new DataRQ()
			Transport transport = new Transport()
			dataRQ.transport = transport
			def ret

		when:
			ret = client.data(dataRQ)

		then:
			assert ret != null
			assert ret.class == DataRS
			assert ret?.transport?.header?.error != null

		when:
			ret = client.data(transport)

		then:
			assert ret != null
			assert ret.class == Transport
			assert ret?.header?.error != null
	}

}
