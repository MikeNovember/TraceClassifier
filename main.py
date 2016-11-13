from os import listdir
from os.path import isfile, isdir, join
import lxml.etree as ET
import datetime
from numpy.random import rand
from math import degrees, atan2
from re import compile
from PIL import Image, ImageDraw, ImageFont

class Trace:
    DATE_FORMAT = "%Y-%m-%d"
    
    def __init__(self):
        pass
    
    def fromElement(self, root):
        if root is None:
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
        self.actual = self.subject.run(*self.args)
        self.passed = (self.actual == self.expected)
        
    def isPassed(self):
        return self.passed
        
    def getExpected(self):
        return self.expected
        
    def getActual(self):
        return self.actual
        
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
        
    def getPercentageMap(self):
        countMap = {}
        percentages = {}
        for testCase in self.testCases:
            if testCase.getExpected() not in countMap:
                countMap[testCase.getExpected()] = [0, 0, {}]
            countMap[testCase.getExpected()][0] += 1 if testCase.isPassed() else 0
            if not testCase.isPassed() and testCase.getActual() != -1:
                if testCase.getActual() not in countMap[testCase.getExpected()][2]:
                    countMap[testCase.getExpected()][2][testCase.getActual()] = 0
                countMap[testCase.getExpected()][2][testCase.getActual()] += 1
            countMap[testCase.getExpected()][1] += 1
        for digit, info in countMap.items():
            percentages[digit] = (info[0] * 100.0 / info[1], info[2])
        return percentages
        
class RandomClassifier:
    def __init__(self):
        pass
    
    def run(self, trace):
        return int(10 * rand(1))

class DirectionalRegExpClassifier:
    def __init__(self, directionalResolution, regexes, rotation=None, debug=False):
        """
        NOTE - regexes must be supplied as list of lists - list under index 0 represents regexes that can be used to recognize
        digit 0 - even if it's only one regex it has to come as one-element list
        
        Directions are simply integers. For example if directional resolution is 4 then:
        0 means "up" or 315 to 45 degrees (0 +- 45)
        1 means "right" or 45 to 135 degrees (90 +- 45)
        2 means "down" or 135 to 225 degrees (180 +- 45)
        3 means "left" or 225 to 315 degrees (270 +- 45)
        left boundary is inclusive
        """
        self.debug = debug
        self.directionAngle = 360/directionalResolution
        if rotation:
            self.rotation = rotation
        else:
            self.rotation = self.directionAngle/2
        self.regexes = {}
        for regexList, digit in zip(regexes, range(len(regexes))):
            self.regexes[digit] = []
            for regex in regexList:
                self.regexes[digit].append(compile(regex))

    def getAngle(self, point1, point2):
        vector = (point2[0] - point1[0], point2[1] - point1[1])
        angle = 90 + degrees(atan2(vector[1], vector[0]))
        if angle < 0:
            angle += 360
        if self.debug:
            self.angle = angle
        return angle
        
    def getDirection(self, point1, point2):
        angle = self.getAngle(point1, point2)
        direction = int((((angle+self.rotation)+360) % 360) / self.directionAngle)
        if self.debug:
            self.direction = direction
        return direction
        
    def makeDirectionString(self, trace):
        string = ""
        points = trace.getPoints()
        point1 = points[0]
        point2 = points[1]
        prevDirection = self.getDirection(point1, point2)
        string += str(prevDirection)
        if self.debug:
            self.image = Image.new('RGB', (1080, 1920), (255, 255, 255))
            self.draw = ImageDraw.Draw(self.image)
            self.font = ImageFont.load_default()
            self.count = 1
            self.draw.line((point1[0], point1[1], point2[0], point2[1]), fill=128)
            self.draw.text(( (point1[0] + point2[0]) / 2.0, (point1[1] + point2[1]) / 2.0 ), "{0}\n{1}".format(prevDirection, int(self.angle)), font=self.font, fill=0)
        point1 = point2
        for point2 in points[2:-1]:
            currentDirection = self.getDirection(point1, point2)
            if self.debug:
                self.draw.line((point1[0], point1[1], point2[0], point2[1]), fill=128)
                self.draw.text(( (point1[0] + point2[0]) / 2.0, (point1[1] + point2[1]) / 2.0 ), "{0}\n{1}".format(self.direction, int(self.angle)), font=self.font, fill=0)
            if currentDirection != prevDirection:
                string += str(currentDirection)
            prevDirection = currentDirection
            point1 = point2
        if self.debug:
            self.draw.text((20, 20), string, font=self.font, fill=0)
            self.image.save("log/{0}.jpeg".format(datetime.datetime.now().strftime("%Y-%m-%d %H-%M-%S.%f")), "JPEG")
            self.count += 1
        return string
        
    def run(self, trace):
        if self.debug:
            self.textLog = open("log/DirectionalRegExp.log", "a")
        traceAsString = self.makeDirectionString(trace)
        if self.debug:
            self.textLog.write("Matching trace string {0}:\n".format(traceAsString))
        for digit, regexes in self.regexes.items():
            if self.debug:
                self.textLog.write("\tTrying {0} regexes for digit {1}:\n".format(len(regexes), digit))
            for regex in regexes:
                if regex.match(traceAsString):
                    if self.debug:
                        self.textLog.write("\t\tRegex {0}, string {1}... matched!\n\n".format(regex.pattern, traceAsString))
                        self.textLog.close()
                    return digit
                if self.debug:
                    self.textLog.write("\t\tRegex {0}, string {1}... not matched\n".format(regex.pattern, traceAsString))
        if self.debug:
            self.textLog.write("\tFailed to match any regexp\n\n")
            self.textLog.close()
        return -1
        
def main():
    repository = FileTraceRepository('repository')
    repository.pull()

    regexes4 = [
    ["^[3]*2103[2]*$"],              #0
    ["^2$", "^0[1]*2$"],             #1
    ["^[3]*012[3]*1[0]*$"],          #2
    ["^[0]*12[3]*[0]*[2]*123[0]*$"], #3
    ["^21[0]*2$"],                   #4
    ["^321[0]*123[0]*$"],            #5
    ["^[0]*32103[2]*$"],             #6
    ["^12$"],                        #7
    ["^[3]*0121[3]*[0]+$"],          #8
    ["^[2]*10321[0]*$"]              #9
    ]
    
    directionalClassifier = DirectionalRegExpClassifier(4, regexes4, debug=True)

    directionalSuite = ClassifierTestSuite(repository, directionalClassifier)
    directionalSuite.run()

    print("Directional classifier: {0}% success".format(directionalSuite.getPassedPercentage()))
    for digit, (percentage, missed) in directionalSuite.getPercentageMap().items():
        print("{0}: {1}% success".format(digit, int(percentage)))
        for value, count in missed.items():
            print("\t{1} times recognized as {0} ".format(value, count))

main()