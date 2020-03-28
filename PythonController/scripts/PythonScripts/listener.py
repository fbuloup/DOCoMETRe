from py4j.java_gateway import JavaGateway, CallbackServerParameters
from py4j.java_collections import SetConverter, MapConverter, ListConverter

import struct, array

class PythonListener(object):

    def __init__(self, gateway):
        self.gateway = gateway
        values=[1.0*i/10000 for i in range(10000)]
        
        print("Before")
        #self.java_values = ListConverter().convert(values, gateway._gateway_client)
        arrayValues = array.array('d', values)
        self.java_values = arrayValues.tobytes()
        print("After")
        
    def notify(self, obj):
        print("Notified by Java")
        print(obj)
        
        gateway.jvm.System.out.println("Hello from python!")

        #ba = bytearray(struct.pack("!d", value))

        return self.java_values

    class Java:
        implements = ["py4j.examples.ExampleListener"]

if __name__ == "__main__":
    gateway = JavaGateway(
        callback_server_parameters=CallbackServerParameters())
    listener = PythonListener(gateway)
    gateway.entry_point.registerListener(listener)
    gateway.entry_point.notifyAllListeners()
    gateway.shutdown()
