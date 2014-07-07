package cn.lois.video.wsplayer;

import java.nio.ByteBuffer;   

public class H264decode {   
       
    private long H264decode = 0;   
       
    static{      
        System.loadLibrary("H264Decode");   
    }   
  
    public H264decode() {   
        this.H264decode = Initialize();   
    }   
       
    public void Cleanup() {   
        Destroy(H264decode);   
    }   
       
    public int DecodeOneFrame(ByteBuffer pInBuffer,ByteBuffer pOutBuffer) {   
        return DecodeOneFrame(H264decode, pInBuffer, pOutBuffer);   
    }   
  
    public int DecodeG726Audio(ByteBuffer pInBuffer,ByteBuffer pOutBuffer) {   
        return DecodeG726Audio(H264decode, pInBuffer, pOutBuffer);   
    }   
  
    private native static int DecodeOneFrame(long H264decode, ByteBuffer pInBuffer, ByteBuffer pOutBuffer);   
    private native static int DecodeG726Audio(long H264decode, ByteBuffer pInBuffer, ByteBuffer pOutBuffer);   
    private native static long Initialize();   
    private native static void Destroy(long H264decode);   
}  
