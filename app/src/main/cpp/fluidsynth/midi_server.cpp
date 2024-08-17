#include <jni.h>
#include <string>
#include <fluidsynth.h>
#include <unistd.h>


#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
//#include <sys/stat.h>
#include <android/log.h>
#include <pthread.h>



#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, "MidiServerJNI", __VA_ARGS__);
#define LENGTH_OF_LISTEN_QUEUE  20
#define BUFFER_SIZE             8

#define TYPE_ON_CONNECT         0x12 //初始连接成功时
#define TYPE_SHORT_MSG          0x13 //midiOutShortMsg时

//取自asoundef.h
#define MIDI_CMD_NOTE_OFF		0x80	/**< note off */
#define MIDI_CMD_NOTE_ON		0x90	/**< note on */
#define MIDI_CMD_NOTE_PRESSURE	0xa0	/**< key pressure */
#define MIDI_CMD_CONTROL		0xb0	/**< control change */
#define MIDI_CMD_PGM_CHANGE		0xc0	/**< program change */
#define MIDI_CMD_CHANNEL_PRESSURE	0xd0	/**< channel pressure */
#define MIDI_CMD_BENDER			0xe0	/**< pitch bender */
#define MIDI_CMD_COMMON_SYSEX	0xf0	/**< sysex (system exclusive) begin */

fluid_settings_t *settings = nullptr;
fluid_synth_t *synth = nullptr;
fluid_audio_driver_t *adriver = nullptr;

int server_socket = -1;
bool isSocketRunning = false;
bool doLog = false;

void* socketStart(void* param);

void fluidSynthStart() {
    printf("初始化fluidsynth, 开始socket监听");
    if (!synth) {
        // Setup synthesizer
        settings = new_fluid_settings();
//        fluid_settings_setint(settings, "synth.midi-channels", 0xff);
//        fluid_settings_setstr(settings, "audio.driver", "oboe");
        synth = new_fluid_synth(settings);
        adriver = new_fluid_audio_driver(settings, synth);

        // Load sample soundfont
        fluid_synth_sfload(synth, getenv("MIDI_SF2_PATH"), 1);
    }

    //socket用一个新线程吧，因为会一直循环不结束.
    if (!isSocketRunning) {
        pthread_t t;
        pthread_create(&t, nullptr, socketStart, nullptr);
        isSocketRunning = true;
    }
}

void fluidSynthClose() {
    if (!synth)
        return;

    delete_fluid_audio_driver(adriver);
    delete_fluid_synth(synth);
    delete_fluid_settings(settings);

    adriver = nullptr;
    synth = nullptr;
    settings = nullptr;

    //关闭server_socket，accept那里被唤醒。结束函数线程
    shutdown(server_socket, SHUT_RDWR);
}


void fluidSynthPlayNote(int channel, int key, int velocity) {
    if (synth)
        fluid_synth_noteon(synth, channel, key, velocity);
}


void fluidSynthPlayTest() {
    fluid_synth_noteon(synth, 0, 60, 127); // play middle C
    sleep(1); // sleep for 1 second
    fluid_synth_noteoff(synth, 0, 60); // stop playing middle C
    fluid_synth_noteon(synth, 0, 62, 127);
    sleep(1);
    fluid_synth_noteoff(synth, 0, 62);
    fluid_synth_noteon(synth, 0, 64, 127);
    sleep(1);
    fluid_synth_noteoff(synth, 0, 64);
}

/**
 * 直接参考alsamidi.c的midi_out_data处理
 */
void handleDataFromShortMsg(char* buf) {

}



void* socketStart(void* param) {
    printf("socketStart开始执行");

    struct sockaddr_in server_addr;
    server_socket;
    int opt = 1;
    int ret = 0;
    int SERVER_PORT = atoi(getenv("MIDI_SOCKET_PORT"));

    bzero(&server_addr, sizeof(server_addr)); // 置字节字符串前n个字节为0，包含'\0'
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK); // 转小端,INADDR_LOOPBACK就是指定地址为127.0.0.1的地址
    server_addr.sin_port = htons(SERVER_PORT);

    // 创建一个Socket
    server_socket = socket(PF_INET, SOCK_STREAM, 0);

    if (server_socket < 0) {
        printf("socket创建失败!\n");
        return nullptr;
    }

    // bind a socket
    setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
    if (bind(server_socket, (struct sockaddr*)&server_addr, sizeof(server_addr))) {
        printf("socket Server 绑定端口: %d 失败! \n",  SERVER_PORT);
        return nullptr;
    }

    while(1)
    {
        if (!synth) {
            printf("synth未工作，无法播放音符。退出\n");
            break;
        }

        // 监听Socket
        if (listen(server_socket, LENGTH_OF_LISTEN_QUEUE))
        {
            printf("Server Listen Failed!\n");
            break;
        }

        sockaddr_in client_addr{};
        int client_socket;
        socklen_t length;
        char buffer[BUFFER_SIZE];
        char channel, status, key, velocity;
        uint32_t dwParam;
        time_t current_time;


        // 连接客户端Socket
        length = sizeof(client_addr);
        client_socket = accept(server_socket, (struct sockaddr*)&client_addr, &length);
        if (client_socket < 0)
        {
            printf("Server Accept Failed!\n");
            break;
        }

        //设置重置一下
        fluid_synth_system_reset(synth);
        //socket断开时部分音符可能未停止播放。手动结束。
        for (int chan = 0; chan < 16; chan ++)
            fluid_synth_all_notes_off(synth, chan);

        // 从客户端接收数据
        while(1)
        {
            bzero(buffer, BUFFER_SIZE);
            buffer[0] = '\0';
            length = recv(client_socket, buffer, BUFFER_SIZE, 0);

            if (length <= 0 || !synth) {
                printf("关闭socket连接。len=%d, synth=%08x\n", length, synth);
                break;
            }

            if (buffer[0] == TYPE_SHORT_MSG) {
                int evt = buffer[1], d1 = buffer[2], d2 = buffer[3];
                if (doLog) printf("接收到数据=%02x, %02x, %02x, len=%d, time=\n", evt & 0x00ff, d1, d2, (int)length);
                switch (evt & 0xF0) {
                    case MIDI_CMD_NOTE_OFF:
                        fluid_synth_noteoff(synth, evt & 0x0F, d1);
                        break;
                    case MIDI_CMD_NOTE_ON:
                        fluid_synth_noteon(synth, evt & 0x0F, d1, d2);
                        break;
                    case MIDI_CMD_NOTE_PRESSURE:
                        fluid_synth_key_pressure(synth, evt & 0x0F, d1, d2);
                        break;
                    case MIDI_CMD_CONTROL:
                        fluid_synth_cc(synth, evt & 0x0F, d1, d2);
                        break;
                    case MIDI_CMD_BENDER:
                        fluid_synth_pitch_bend(synth, evt * 0x0F, d1);
                        break;
                    case MIDI_CMD_PGM_CHANGE:
                        fluid_synth_program_change(synth, evt & 0x0F, d1);
                        break;
                    case MIDI_CMD_CHANNEL_PRESSURE:
                        fluid_synth_channel_pressure(synth, evt & 0x0F, d1);
                        break;
                    case MIDI_CMD_COMMON_SYSEX:
                        printf("尚不支持evt类型MIDI_CMD_COMMON_SYSEX\n");
                        break;

                }
            } else if (buffer[0] == TYPE_ON_CONNECT) {
                printf("握手\n");
            } else {
                printf("接收到socket消息类型未知:%02x , 忽略\n", buffer[0] & 0x00ff);
            }
        }
        //socket断开时部分音符可能未停止播放。手动结束。
        for (int chan = 0; chan < 16; chan ++)
            fluid_synth_all_notes_off(synth, chan);

        printf("关闭client_socket\n");
        close(client_socket);
    }

    close(server_socket);
    isSocketRunning = false;
    printf("socketStart执行完毕, 线程结束\n");
    return nullptr;
}



extern "C"
JNIEXPORT void JNICALL
Java_com_ewt45_winlator_E14_1MidiServer_fluidSynthPlayTest(JNIEnv *env, jclass clazz) {
    printf("播放midi测试,三个音符\n");
    fluidSynthPlayTest();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_ewt45_winlator_E14_1MidiServer_fluidSynthClose(JNIEnv *env, jclass clazz) {
    fluidSynthClose();
}


extern "C"
JNIEXPORT void JNICALL
Java_com_ewt45_winlator_E14_1MidiServer_fluidSynthStart(JNIEnv *env, jclass clazz) {
    fluidSynthStart();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_ewt45_winlator_E14_1MidiServer_fluidSynthAllNoteOff(JNIEnv *env, jclass clazz) {
    if (!synth)
        return;
    printf("应用切至后台，停止正在播放的音符\n");
    for (int i = 0; i < 16; i ++)
        fluid_synth_all_notes_off(synth, i);
}