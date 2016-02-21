package com.bytezone.diskbrowser.applefile;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.bytezone.diskbrowser.HexFormatter;

public class HiResImage extends AbstractFile
{
  private static final int WHITE = 0xFFFFFF;
  private static final int BLACK = 0x000000;
  private static final int RED = 0xFF0000;
  private static final int GREEN = 0x00FF00;
  private static final int BLUE = 0x0000FF;
  private static final int VIOLET = 0xBB66FF;
  private static final int[][] colours = { { VIOLET, GREEN }, { BLUE, RED } };

  private static int[] line = new int[280];

  private int fileType;
  private int auxType;
  private byte[] unpackedBuffer;
  private static boolean colourQuirks;

  public HiResImage (String name, byte[] buffer)
  {
    super (name, buffer);

    if (name.equals ("FLY LOGO") || name.equals ("BIGBAT.PAC"))
    {
      this.buffer = unscrunch (buffer);
    }
    if (isGif (buffer))
      makeGif ();
    else if (true)
      drawColour (buffer);
    else
      drawMonochrome (buffer);
  }

  public HiResImage (String name, byte[] buffer, int fileType, int auxType)
  {
    super (name, buffer);
    this.fileType = fileType;
    this.auxType = auxType;

    if (fileType == 0xC0 && auxType == 1)
    {
      unpackedBuffer = unpackBytes (buffer);
      makeScreen2 (unpackedBuffer);
    }
    if (fileType == 0xC0 && auxType == 2)
    {
      System.out.println ("yippee - Preferred picture format - " + name);
    }
  }

  public static void setDefaultColourQuirks (boolean value)
  {
    colourQuirks = value;
  }

  public void setColourQuirks (boolean value)
  {
    if (colourQuirks == value)
      return;

    colourQuirks = value;
    drawColour (buffer);
  }

  private void drawMonochrome (byte[] buffer)
  {
    int rows = buffer.length <= 8192 ? 192 : 384;
    image = new BufferedImage (280, rows, BufferedImage.TYPE_BYTE_GRAY);

    DataBuffer db = image.getRaster ().getDataBuffer ();

    int element = 0;
    byte[] mask = { 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01 };

    for (int z = 0; z < rows / 192; z++)
      for (int i = 0; i < 3; i++)
        for (int j = 0; j < 8; j++)
          for (int k = 0; k < 8; k++)
          {
            int base = i * 0x28 + j * 0x80 + k * 0x400 + z * 0x2000;
            int max = Math.min (base + 40, buffer.length);
            for (int ptr = base; ptr < max; ptr++)
            {
              byte val = buffer[ptr];
              if (val == 0) // no pixels set
              {
                element += 7;
                continue;
              }
              for (int bit = 6; bit >= 0; bit--)
              {
                if ((val & mask[bit]) > 0)
                  db.setElem (element, 255);
                element++;
              }
            }
          }
  }

  private void drawColour (byte[] buffer)
  {
    int rows = buffer.length <= 8192 ? 192 : 384;
    image = new BufferedImage (280, rows, BufferedImage.TYPE_INT_RGB);

    DataBuffer db = image.getRaster ().getDataBuffer ();
    int element = 0;

    for (int z = 0; z < rows / 192; z++)
      for (int i = 0; i < 3; i++)
        for (int j = 0; j < 8; j++)
          for (int k = 0; k < 8; k++)
          {
            int base = i * 0x28 + j * 0x80 + k * 0x400 + z * 0x2000;
            element = drawLine (db, element, base);
          }
  }

  private int drawLine (DataBuffer db, int element, int base)
  {
    int max = Math.min (base + 40, buffer.length);
    int linePtr = 0;

    for (int ptr = base; ptr < max; ptr++)
    {
      int colourBit = (buffer[ptr] & 0x80) >> 7;
      int value = buffer[ptr] & 0x7F;

      for (int px = 0; px < 7; px++)
      {
        int val = (value >> px) & 0x01;
        int column = (ptr + px) % 2;
        line[linePtr++] = val == 0 ? 0 : colours[colourBit][column];
      }
    }

    // convert ALL consecutive ON pixels to white
    if (true)
      for (int x = 0; x < line.length - 1; x++)
        if (line[x] != BLACK && line[x + 1] != BLACK)
          //        if (isColoured (line[x]) && isColoured (line[x + 1]))
          line[x] = line[x + 1] = WHITE;

    // convert WOZ quirks
    if (colourQuirks)
    {
      for (int x = 0; x < line.length - 3; x++)
      {
        int px0 = line[x];
        int px1 = line[x + 1];
        int px2 = line[x + 2];
        int px3 = line[x + 3];

        if (px1 == BLACK)
        {
          if (px3 == BLACK && px0 == px2 && isColoured (px0))           //     V-B-V-B
            line[x + 1] = px0;                                          // --> V-V-V-B

          else if (px3 == WHITE && px2 == WHITE && isColoured (px0))    //     V-B-W-W
            line[x + 1] = px0;                                          // --> V-V-W-W
        }
        else if (px2 == BLACK)
        {
          if (px0 == BLACK && px1 == px3 && isColoured (px3))           //     B-G-B-G 
            line[x + 2] = px3;                                          // --> B-G-G-G

          else if (px0 == WHITE && px1 == WHITE && isColoured (px3))    //     W-W-B-G
            line[x + 2] = px3;                                          // --> W-W-G-G
        }
      }
    }

    for (int pixel : line)
      db.setElem (element++, pixel);

    return element;
  }

  private boolean isColoured (int pixel)
  {
    return pixel != BLACK && pixel != WHITE;
  }

  private void makeScreen2 (byte[] buffer)
  {
    //    System.out.println (HexFormatter.format (buffer, 32000, 640));
    //    for (int table = 0; table < 200; table++)
    //    {
    //      System.out.println (HexFormatter.format (buffer, ptr, 32));
    //      for (int color = 0; color < 16; color++)
    //      {
    //        int red = buffer[ptr++] & 0x0F;
    //        int green = (buffer[ptr] & 0xF0) >> 4;
    //        int blue = buffer[ptr++] & 0x0F;
    //        Color c = new Color (red, green, blue);
    //      }
    //    }

    image = new BufferedImage (320, 200, BufferedImage.TYPE_BYTE_GRAY);
    DataBuffer db = image.getRaster ().getDataBuffer ();

    int element = 0;
    int ptr = 0;
    for (int row = 0; row < 200; row++)
      for (int col = 0; col < 160; col++)
      {
        int pix1 = (buffer[ptr] & 0xF0) >> 4;
        int pix2 = buffer[ptr] & 0x0F;
        if (pix1 > 0)
          db.setElem (element, 255);
        if (pix2 > 0)
          db.setElem (element + 1, 255);
        element += 2;
        ptr++;
      }
  }

  // Beagle Bros routine to expand a hi-res screen
  private byte[] unscrunch (byte[] src)
  {
    //    byte[] dst = new byte[src.length < 0x2000 ? 0x2000 : 0x4000];
    byte[] dst = new byte[0x2000];
    int p1 = 0;
    int p2 = 0;
    //    while (p1 < dst.length && p2 < src.length)
    while (p1 < dst.length)
    {
      byte b = src[p2++];
      if ((b == (byte) 0x80) || (b == (byte) 0xFF))
      {
        b &= 0x7F;
        int rpt = src[p2++];
        for (int i = 0; i < rpt; i++)
          dst[p1++] = b;
      }
      else
        dst[p1++] = b;
    }
    return dst;
  }

  private void makeGif ()
  {
    try
    {
      image = ImageIO.read (new ByteArrayInputStream (buffer));
    }
    catch (IOException e)
    {
      e.printStackTrace ();
    }
  }

  private byte[] unpackBytes (byte[] buffer)
  {
    // routine found here - http://kpreid.livejournal.com/4319.html

    byte[] newBuf = new byte[32768];
    byte[] fourBuf = new byte[4];

    //    System.out.println (HexFormatter.format (buffer));

    int ptr = 0, newPtr = 0;
    while (ptr < buffer.length)
    {
      int type = (buffer[ptr] & 0xC0) >> 6;
      int count = (buffer[ptr++] & 0x3F) + 1;

      switch (type)
      {
        case 0:
          while (count-- != 0)
            newBuf[newPtr++] = buffer[ptr++];
          break;

        case 1:
          byte b = buffer[ptr++];
          while (count-- != 0)
            newBuf[newPtr++] = b;
          break;

        case 2:
          for (int i = 0; i < 4; i++)
            fourBuf[i] = buffer[ptr++];
          while (count-- != 0)
            for (int i = 0; i < 4; i++)
              newBuf[newPtr++] = fourBuf[i];
          break;

        case 3:
          b = buffer[ptr++];
          count *= 4;
          while (count-- != 0)
            newBuf[newPtr++] = b;
          break;
      }
    }

    return newBuf;
  }

  @Override
  public String getText ()
  {
    String auxText = "";
    StringBuilder text = new StringBuilder ("Image File : " + name);
    text.append (String.format ("%nFile type  : $%02X", fileType));

    switch (fileType)
    {
      case 8:
        if (auxType < 0x4000)
          auxText = "Graphics File";
        else if (auxType == 0x4000)
          auxText = "Packed Hi-Res File";
        else if (auxType == 0x4001)
          auxText = "Packed Double Hi-Res File";
        break;

      case 192:
        if (auxType == 1)
        {
          if (unpackedBuffer == null)
            unpackedBuffer = unpackBytes (buffer);
          auxText = "Packed Super Hi-Res Image";
        }
        else if (auxType == 2)
          auxText = "Super Hi-Res Image";
        else if (auxType == 3)
          auxText = "Packed QuickDraw II PICT File";
        break;

      case 193:
        if (auxType == 0)
          auxText = "Super Hi-res Screen Image";
        else if (auxType == 1)
          auxText = "QuickDraw PICT File";
        else if (auxType == 2)
          auxText = "Super Hi-Res 3200 color image";
    }

    if (!auxText.isEmpty ())
      text.append (String.format ("%nAux type   : $%04X  %s", auxType, auxText));

    text.append (String.format ("%nFile size  : %,d", buffer.length));
    if (unpackedBuffer != null)
    {
      text.append (String.format ("%nUnpacked   : %,d%n%n", unpackedBuffer.length));
      text.append (HexFormatter.format (unpackedBuffer));
    }

    return text.toString ();
  }

  public static boolean isGif (byte[] buffer)
  {
    if (buffer.length < 6)
      return false;
    String text = new String (buffer, 0, 6);
    return text.equals ("GIF89a") || text.equals ("GIF87a");
  }
}