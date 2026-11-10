// Host test of the generated parameters code (see ArduinoUnoParametersCodeTest) : Serial is mocked,
// int can be forced to 16 bits range (-DINT16) like on Arduino Uno R3. Exit code is the number of failures.
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <cerrno>
#include <climits>
#include <string>
#include <cassert>
typedef unsigned char byte;
#ifdef INT16
#undef INT_MIN
#undef INT_MAX
#define INT_MIN (-32768)
#define INT_MAX 32767
#endif
struct MockSerial {
	std::string input; size_t pos = 0; std::string output;
	int available() { return pos < input.size(); }
	int read() { return input[pos++]; }
	void print(char c) { output += c; }
	void println(int v) { output += std::to_string(v) + "\r\n"; }
	void println(const char* s) { output += std::string(s) + "\r\n"; }
	void flush() {}
} Serial;
bool startLoop;
bool pBool; char pChar; int pInt; long pLong; float pFloat; char pString[9];
#include "generated.inc"
static std::string run(const std::string& in) {
	Serial.input = in; Serial.pos = 0; Serial.output = ""; startLoop = false;
	waitForStart();
	return Serial.output;
}
#define CHECK(c) do { if(!(c)) { printf("FAIL line %d : %s\n", __LINE__, #c); failures++; } } while(0)
int main() {
	int failures = 0;
	std::string ok = "P1:1\nP2:x\nP3:-32768\nP4:-2147483648\nP5:+1.500000E+00\nP6:cond-1\nE6\n";
	CHECK(run(ok + "r") == "A6\r\n");
	CHECK(pBool == true && pChar == 'x' && pInt == -32768 && pLong == -2147483648L && pFloat == 1.5f && strcmp(pString, "cond-1") == 0);
	CHECK(parametersAcknowledged);
	// CR LF, any order
	CHECK(run("P6:abc\r\nP5:-2.500000E-03\r\nP4:7\r\nP3:12\r\nP2:z\r\nP1:0\r\nE6\r\nr") == "A6\r\n");
	CHECK(pBool == false && pChar == 'z' && pInt == 12 && pLong == 7 && pFloat == -0.0025f && strcmp(pString, "abc") == 0);
	// Missing parameter : N3, then a new sending is acknowledged
	CHECK(run("P1:1\nP2:x\nP4:1\nP5:+1.0E+00\nP6:a\nE6\n" + ok + "r") == "N3\r\nA6\r\n");
	// Invalid values
	CHECK(run("P1:2\nP2:x\nP3:1\nP4:1\nP5:1\nP6:a\nE6\nr") == "N1\r\nN0\r\n");
	CHECK(run("P1:1\nP2:xy\nP3:1\nP4:1\nP5:1\nP6:a\nE6\nr").substr(0, 4) == "N2\r\n");
	CHECK(run("P1:1\nP2:x\nP3:abc\nP4:1\nP5:1\nP6:a\nE6\nr").substr(0, 4) == "N3\r\n");
	CHECK(run("P1:1\nP2:x\nP3:1\nP4:1\nP5:1.5x\nP6:a\nE6\nr").substr(0, 4) == "N5\r\n");
	CHECK(run("P1:1\nP2:x\nP3:1\nP4:1\nP5:1\nP6:123456789\nE6\nr").substr(0, 4) == "N6\r\n");
	CHECK(run("P1:1\nP2:x\nP3:1\nP4:99999999999999999999\nP5:1\nP6:a\nE6\nr").substr(0, 4) == "N4\r\n");
	// Unknown parameter, wrong count
	CHECK(run(ok.substr(0, ok.size() - 3) + "P7:1\nE6\nr").substr(0, 4) == "N7\r\n");
	CHECK(run(ok.substr(0, ok.size() - 3) + "E5\nr").substr(0, 4) == "N0\r\n");
	// No parameter sent before 'r' : N0 (process to compile again), start anyway
	CHECK(run("r") == "N0\r\n");
	CHECK(startLoop);
	// Too long line ignored : parameter missing
	CHECK(run("P1:1\nP2:x\nP3:1\nP4:1\nP5:1\nP6:" + std::string(100, 'a') + "\nE6\nr").substr(0, 4) == "N6\r\n");
	// 'r' inside a line is not the start
	CHECK(run("P6:rr\nP1:1\nP2:r\nP3:1\nP4:1\nP5:1\nE6\nr") == "A6\r\n");
	CHECK(pChar == 'r' && strcmp(pString, "rr") == 0);
	printf(failures == 0 ? "ALL OK\n" : "%d FAILURES\n", failures);
	return failures;
}
