from py4j.java_gateway import JavaGateway, CallbackServerParameters
from py4j.java_collections import SetConverter, MapConverter, ListConverter

import struct, array

class Experiment(object):
    
    def __init__(self):
        self.data = {} 
    
    def __getitem__(self, key): 
        return self.data[key]
    
    def __setitem__(self, key, item):
        self.data[key] = item