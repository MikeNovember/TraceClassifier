from os import listdir
from os.path import isfile, isdir, join
import lxml.etree as ET
import datetime
from numpy.random import rand

class Trace:
    DATE_FORMAT = "%Y-%m-%d"
    
    def __init__(self):
        pass
    
    def fromElement(self, root):
        if root is not None:
            return            
        assert (root.tag == 'trace'), 'Invalid trace element'
        self.date = datetime.datetime.strptime(root.get('date'), self.DATE_FORMAT)
        self.ms = int(root.get('ms'));
        self.points = []
        for point in root.iter('point'):
            x = float(point.get('x'))
            y = float(point.get('y'))
            t = float(point.get('t'))
            self.points.append((x, y, t))
        
    def getPoints(self):
        return self.points
        
class FileTraceRepository:
    def __init__(self, path):
        self.path = path
        self.traces = {}
    
    def pull(self):
        subdirectories = [filename for filename in listdir(self.path) if isdir(join(self.path, filename))]
        if not subdirectories:
            return
        for subdirectory in subdirectories:
            self.traces[subdirectory] = []
            filenames = [filename for filename in listdir(join(self.path, subdirectory)) if isfile(join(self.path, subdirectory, filename))];
            for filename in filenames:
                tree = ET.parse(join(self.path, subdirectory, filename))
                root = tree.getroot()
                trace = Trace()
                trace.fromElement(root)
                self.traces[subdirectory].append(trace)
                
    def getTraces(self):
        return self.traces
        
class TestCase:
    def __init__(self, subject, expected, *args):
        """
        subject - object to be tested
        expected - expected test result
        *args - arguments to pass to subject's run() method
        """
        self.subject = subject
        self.expected = expected
        self.args = args

    def run(self):
        self.actual = self.subject.run(self.args)
        self.passed = self.actual == self.expected
        
    def isPassed(self):
        return self.passed
        
class ClassifierTestSuite:
    def __init__(self, repository, classifier):
        self.repository = repository
        self.classifier = classifier
        self.testCases = []
        for tag, traces in self.repository.getTraces().items():
            for trace in traces:
                self.testCases.append(TestCase(self.classifier, int(tag), trace))
        self.passed = 0
        self.total = len(self.testCases)
        
    def run(self):
        for testCase in self.testCases:
            testCase.run()
            if testCase.isPassed():
                self.passed += 1
            self.total += 1
            
    def getPassedPercentage(self):
        return self.passed * 100.0 / self.total
        
    def getFailedPercentage(self):
        return (self.total - self.passed) * 100.0 / self.total
        
    def getPassedCount(self):
        return self.passed
        
    def getFailedCount(self):
        return self.total - self.passed
        
    def getTotalCount(self):
        return self.total
        
class RandomClassifier:
    def __init__(self):
        pass
    
    def run(self, trace):
        return int(10 * rand(1))
        
def main():
    repository = FileTraceRepository('pathcollector')
    repository.pull()
    randomClassifier = RandomClassifier()
    randomSuite = ClassifierTestSuite(repository, randomClassifier)
    randomSuite.run()
    
    print("Random classifier: {0}% success".format(randomSuite.getPassedPercentage()))
    
main()