#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <time.h>
#include <unistd.h>

}

const char *TAG = __FILE__;

jobject gCallback;
jmethodID gCallbackMethodId;
bool isStop = false;

void callback(JNIEnv *env, uint8_t *buf, int channel, int width, int height);

// from android samples
/* return current time in milliseconds */
static double now_ms(void) {

    struct timespec res;
    clock_gettime(CLOCK_REALTIME, &res);
    return 1000.0 * res.tv_sec + (double) res.tv_nsec / 1e6;

}

extern "C"
jint
Java_com_potterhsu_rtsplibrary_RtspClient_initialize(
        JNIEnv *env,
        jobject,
        jobject callback) {
    gCallback = env->NewGlobalRef(callback);
    jclass clz = env->GetObjectClass(gCallback);
    if (clz == NULL) {
        return JNI_ERR;
    } else {
        gCallbackMethodId = env->GetMethodID(clz, "onFrame", "([BIII)V");
        return JNI_OK;
    }
}

extern "C"
jint
Java_com_potterhsu_rtsplibrary_RtspClient_play(
        JNIEnv *env,
        jobject,
        jstring endpoint) {
    SwsContext *img_convert_ctx;
    AVFormatContext* context = avformat_alloc_context();
    AVCodecContext* ccontext = avcodec_alloc_context3(NULL);
    int video_stream_index = -1;


//    context->flags |= AVFMT_FLAG_NOBUFFER;
//    ccontext->max_b_frames = 1;
//    ccontext->thread_count = 4;
//    ccontext->thread_type = FF_THREAD_SLICE;
//    ccontext->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL;
    ccontext->flags |= CODEC_FLAG_LOW_DELAY;
    av_register_all();
    avformat_network_init();

    AVDictionary *option = NULL;
    av_dict_set(&option, "rtsp_transport", "udp", 0);
    av_dict_set(&option, "fflags", "nobuffer", 0);
    av_dict_set(&option, "flags", "low_delay", 0);
    av_dict_set(&option, "framedrop", "1", 0);
    av_dict_set(&option, "islive", "1", 0);
    av_dict_set(&option, "max_delay", "0", 0);
    av_dict_set(&option, "reorder_queue_size", "0", 0);
    av_dict_set(&option, "start-on-prepared", "1", 0);
    av_dict_set(&option, "flush_packets", "1", 0);
    av_dict_set(&option, "sync", "ext", 0);
    av_dict_set(&option, "skip_frame", "0", 0);
    av_dict_set(&option, "skip_loop_filter", "0", 0);



//    av_dict_set(&option, "me_method", "zero", 0);
//    av_dict_set(&option, "tune", "zerolatency", 0);
//    av_dict_set(&option, "tune", "fastdecode", 0);
//    av_dict_set(&option, "timeout", "200", 0);

    // Open RTSP
    const char *rtspUrl= env->GetStringUTFChars(endpoint, JNI_FALSE);

    if (int err = avformat_open_input(&context, rtspUrl, NULL, &option) != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Cannot open input %s, error code: %d", rtspUrl, err);
        return JNI_ERR;
    }
    env->ReleaseStringUTFChars(endpoint, rtspUrl);

    av_dict_free(&option);

    if (avformat_find_stream_info(context, NULL) < 0){
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Cannot find stream info");
        return JNI_ERR;
    }

    // Search video stream
    for (int i = 0; i < context->nb_streams; i++) {
        if (context->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO)
            video_stream_index = i;
    }

    if (video_stream_index == -1) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Video stream not found");
        return JNI_ERR;
    }



    AVPacket packet;
    av_init_packet(&packet);

    // Open output file
    AVFormatContext *oc = avformat_alloc_context();
    AVStream *stream = NULL;

//    // Start reading packets from stream and write them to file
//    av_read_play(context);

    AVCodec *codec = NULL;
    codec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (!codec) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Cannot find decoder H264");
        return JNI_ERR;
    }

    avcodec_get_context_defaults3(ccontext, codec);
//    ccontext->thread_count = 4;
//    ccontext->max_b_frames = 1;
//    ccontext->thread_type = FF_THREAD_SLICE;
//    ccontext->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL;
    ccontext->flags |= CODEC_FLAG_LOW_DELAY;
    avcodec_copy_context(ccontext, context->streams[video_stream_index]->codec);

    if (avcodec_open2(ccontext, codec, NULL) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Cannot open codec");
        return JNI_ERR;
    }

    img_convert_ctx = sws_getContext(ccontext->width, ccontext->height, ccontext->pix_fmt, ccontext->width, ccontext->height,
                                     AV_PIX_FMT_ARGB, SWS_BICUBIC, NULL, NULL, NULL);

    size_t size = (size_t) avpicture_get_size(AV_PIX_FMT_YUV420P, ccontext->width, ccontext->height);
    uint8_t *picture_buf = (uint8_t*)(av_malloc(size));
    AVFrame *pic = av_frame_alloc();
    AVFrame *picrgb = av_frame_alloc();
    size_t size2 = (size_t) avpicture_get_size(AV_PIX_FMT_ARGB, ccontext->width, ccontext->height);
    uint8_t *picture_buf2 = (uint8_t*)(av_malloc(size2));
    avpicture_fill( (AVPicture*) pic, picture_buf, AV_PIX_FMT_YUV420P, ccontext->width, ccontext->height );
    avpicture_fill( (AVPicture*) picrgb, picture_buf2, AV_PIX_FMT_ARGB, ccontext->width, ccontext->height );

    isStop = false;
    double last_start = 0;

//    while(!isStop && av_read_frame(context, &packet) >= 0){
//        if (packet.stream_index == video_stream_index) {
//            if (stream == NULL) {
//                __android_log_print(ANDROID_LOG_ERROR, TAG, "Stream is NULL");
//                stream = avformat_new_stream(oc, context->streams[video_stream_index]->codec->codec);
//                avcodec_copy_context(stream->codec, context->streams[video_stream_index]->codec);
//                stream->sample_aspect_ratio = context->streams[video_stream_index]->codec->sample_aspect_ratio;
//            }
//            int check = 0;
//            packet.stream_index = stream->id;
//
//            int ret;
//            if (&packet) {
//                ret = avcodec_send_packet(ccontext, &packet);
//                // In particular, we don't expect AVERROR(EAGAIN), because we read all
//                // decoded frames with avcodec_receive_frame() until done.
//                if (ret < 0)
//                    check = ret == AVERROR_EOF ? 0 : ret;
//            }
//            ret = avcodec_receive_frame(ccontext, pic);
//            if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF)
//                check = 0;
//            if (ret >= 0)
//                check = 1;
//
////            if(now_ms() - last_start < 100)
////                continue;
////            last_start = now_ms();
//            if(check) {
//                sws_scale(img_convert_ctx, (const uint8_t *const *) pic->data, pic->linesize, 0,
//                          ccontext->height, picrgb->data, picrgb->linesize);
//                callback(env, picture_buf2, 4, ccontext->width, ccontext->height);
//            } else {
//                __android_log_print(ANDROID_LOG_ERROR, TAG, "check: %d, ret: %d", check, ret);
////                    avcodec_flush_buffers(ccontext);
//            }
//
//            av_packet_unref(&packet);
//        }
    while (!isStop ) {
        last_start = now_ms();
        int read_result = av_read_frame(context, &packet);
        if (read_result < 0) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "av_read_frame: %s", av_err2str(read_result));
            break;
        }
        if (packet.stream_index == video_stream_index) { // Packet is video
            if (stream == NULL) {
                __android_log_print(ANDROID_LOG_ERROR, TAG, "Stream is NULL");
                stream = avformat_new_stream(oc, context->streams[video_stream_index]->codec->codec);
                avcodec_copy_context(stream->codec, context->streams[video_stream_index]->codec);
                stream->sample_aspect_ratio = context->streams[video_stream_index]->codec->sample_aspect_ratio;
            }

            int check = 0;
            packet.stream_index = stream->id;
            avcodec_decode_video2(ccontext, pic, &check, &packet);

            if(check) {
                sws_scale(img_convert_ctx, (const uint8_t *const *) pic->data, pic->linesize, 0,
                          ccontext->height, picrgb->data, picrgb->linesize);
//            if(now_ms() - last_start < 100)
//                continue;
//            last_start = now_ms();
                callback(env, picture_buf2, 4, ccontext->width, ccontext->height);
            }
//            __android_log_print(ANDROID_LOG_ERROR, TAG, "NDK loop: %d", now_ms() - last_start);

        }
        av_packet_unref(&packet);
        av_init_packet(&packet);
    }

    av_free(pic);
    av_free(picrgb);
    av_free(picture_buf);
    av_free(picture_buf2);

    av_read_pause(context);
    avio_close(oc->pb);
    avformat_free_context(oc);
    avformat_close_input(&context);

    return isStop ? JNI_OK : JNI_ERR;
}

extern "C"
void
Java_com_potterhsu_rtsplibrary_RtspClient_stop(
        JNIEnv *env,
        jobject) {
    isStop = true;
}

extern "C"
void
Java_com_potterhsu_rtsplibrary_RtspClient_dispose(
        JNIEnv *env,
        jobject) {
    env->DeleteGlobalRef(gCallback);
}

void callback(JNIEnv *env, uint8_t *buf, int nChannel, int width, int height) {
    int len = nChannel * width * height;
    jbyteArray gByteArray = env->NewByteArray(len);
    env->SetByteArrayRegion(gByteArray, 0, len, (jbyte *) buf);
    env->CallVoidMethod(gCallback, gCallbackMethodId, gByteArray, nChannel, width, height);
    env->DeleteLocalRef(gByteArray);
}

