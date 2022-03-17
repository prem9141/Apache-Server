import java.net.InetAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.lang.System;
import java.io.File;
import java.net.UnknownHostException;
import java.net.InetSocketAddress;

class Apache {

  private byte [] HOST; //To store Hostname     
  private int PORT; //To bind ServerSocket
  private InetAddress PREFERRED ; //To store Preferred IP
  private int rlength,hlength,rescode,filesize,urlstart,urlend;
  private byte[] rheader; //To store request header
  
public static void main(String [] a)
{ 
  Apache apache = new Apache(Integer.parseInt(a[0]));
  apache.run(20);
}
Apache(int port) {
  PORT = port;
  HOST = new byte[1024];
  PREFERRED = null;
  file = null;
  // other init stuff ADC here
  rheader = new byte[65535];
  rescode = 0;
  urlstart = 0;
  urlend = 0;
  filesize = 0;
}

// Note: parse() must set HOST correctly
int parse(byte [] buffer)
{ 
  int i=0,offset=0,flag=0,eflag=1;
   
  if(buffer[0]=='G'&&buffer[1]=='E'&&buffer[2]=='T'&&buffer[3]==' ') //check Request Line if GET Method
  {//check Scheme
    if((buffer[4]=='h'&&buffer[5]=='t'&&buffer[6]=='t'&&buffer[7]=='p'
       &&buffer[8]==':'&&buffer[9]=='/'&&buffer[10]=='/') ||
       (buffer[4]=='/')||(buffer[4]=='f'&&buffer[5]=='t'&&buffer[6]=='p'&&buffer[7]==':'&&buffer[8]=='/'&&buffer[9]=='/'))
    {
      i=4;
      if(buffer[i]=='h')
        urlstart = 11;
      else if(buffer[i]=='f')
        urlstart = 10;
      else
        urlstart = 4;
      while(buffer[i++]!=' ')
      { //read url ; 
      }
      urlend = i - 1;
     
      if(buffer[i]=='H'&&buffer[i+1]=='T'&&buffer[i+2]=='T'&&buffer[i+3]=='P'&&buffer[i+4]=='/') //check HTTP Protocol
      {
        if(buffer[i+5]=='1'&&buffer[i+6]=='.'&&buffer[i+7]=='1') //check protocol version
        {
	  if(buffer[i+8]=='\r'&&buffer[i+9]=='\n')
	  {
	    i=i+9;
	    while(++i!=rlength)
	    {
	      if(buffer[i-1]=='\n'&&buffer[i-2]=='\r'&&buffer[i]=='H'&&buffer[i+1]=='o'
	         &&buffer[i+2]=='s'&&buffer[i+3]=='t'&&buffer[i+4]==':')
	      {    
	        i=i+6;
                offset=i;
	        if((buffer[offset]>=58&&buffer[offset]<=64)||(buffer[offset]>=91&&buffer[offset]<=96)
	          ||(buffer[offset]>=123)||(buffer[offset]<=47)) // Check HostName First Character
	        {
		 
	          rescode=400; //Host Name starts with invalid character
	          i++;
	        } 
	        while(buffer[i]!='\r' && buffer[i+1]!='\n' && ((i+2)!=rlength))
	        { 
		  if((buffer[i]<=44)||(buffer[i]==47)||(buffer[i]>=58&&buffer[i]<=64)
		  ||(buffer[i]>=91&&buffer[i]<=96)||buffer[i]>=123) // Check HostName Characters
	            rescode=400;	  //HostName has invalid character
		  i++;
	        } 
	        setHost(buffer,offset,i-1);
	        ++i;
		flag=1;
	      }
	      if(buffer[i]==':')
	      {  
	        if(buffer[i-1]<=64||(buffer[i-1]>=91&&buffer[i-1]<=96)||buffer[i-1]>=123)
		  rescode=400;
	        else
		{
	          eflag=0;
		  ecount++;
	        }
	      }
	      if(buffer[i]=='\r'&&buffer[i+1]=='\n')
		{ 
		  eflag=1;
		  ecount=0;
		}
	    }  
            if(flag==0||eflag==0)
            {  rescode=400; } // Host field is not available
	  }
          else
            rescode=400; // GET Method didn't end properly
        }
        else
          rescode=505; // HTTP Protocol version not supported
      }
      else
        rescode=400; // HTTP Protocol not specified or specified incorrectly
    }
    else
      rescode=400; //GET not followed by http or /
  }
  else if((buffer[0]=='P'&&buffer[1]=='O'&&buffer[2]=='S'&&buffer[3]=='T'&&buffer[4]==' ')|| 
  	  (buffer[0]=='H'&&buffer[1]=='E'&&buffer[2]=='A'&&buffer[3]=='D'&&buffer[4]==' ')|| 
          (buffer[0]=='T'&&buffer[1]=='R'&&buffer[2]=='A'&&buffer[3]=='C'&&buffer[4]=='E'&&buffer[5]==' ')|| 
          (buffer[0]=='P'&&buffer[1]=='U'&&buffer[2]=='T'&&buffer[3]==' ')|| 
          (buffer[0]=='D'&&buffer[1]=='E'&&buffer[2]=='L'&&buffer[3]=='E'&&buffer[4]=='T'&&buffer[5]=='E'&&buffer[6]==' ')|| 
          (buffer[0]=='C'&&buffer[1]=='O'&&buffer[2]=='N'&&buffer[3]=='N'&&buffer[4]=='E'&&buffer[5]=='C'&&buffer[6]=='T'&&buffer[7]==' ')) 
    rescode=501; // Method not implemented	
  else
    rescode=400; // Bad Request
 return rescode;
}

int dns(int X)
{
  long startClock,stopClock,delay=1000;
  int ipindex=0;
  try
  {
    InetAddress[] ip = InetAddress.getAllByName(byte2str(HOST,0,hlength));
    for(int i=0;i<ip.length;i++)
    { 
      try
      { 
        startClock = System.currentTimeMillis();
        Socket s = new Socket();
        s.connect(new InetSocketAddress(ip[i].getHostAddress(),80)); //connect socket to IP
        if(s.isConnected())
        {
    	  stopClock = System.currentTimeMillis();
	  if(delay>(stopClock-startClock)) //check delay
	  {
	    delay=stopClock-startClock;
	    ipindex=i;
	  }  
	}
        s.close();
      }
      catch(Exception e){}
    }
    PREFERRED=ip[ipindex]; //set IP with low delay as Preferred
  }
  catch(Exception e)
  {
    rescode=502;
  }
  return rescode;
}

int http_fetch(Socket client) // NOC - don't change signature
{
  int http_rbytes=0,http_fileSize = 0,sizeflag=0,isalive=0;
  Socket https = null;
  InputStream httpin = null;
  OutputStream clientout,httpout = null;
  byte[] httpbuffer = new byte[1500];
  byte[] cclose = {'C','o','n','n','e','c','t','i','o','n',':',' ','c','l','o','s','e','\r','\n'};
  try
  {
    https = new Socket();
    https.connect(new InetSocketAddress(PREFERRED,80));
    clientout = client.getOutputStream();
    if(https.isConnected())
    {
      
      httpin = https.getInputStream();
      httpout = https.getOutputStream();

      for(int i=0;i<rlength;i++)
      {
        if(rheader[i]=='C'&&rheader[i+1]=='o'&&rheader[i+2]=='n'&&rheader[i+3]=='n'
	   &&rheader[i+4]=='e'&&rheader[i+5] =='c'&&rheader[i+6]=='t'&&rheader[i+7]=='i'&&rheader[i+8]=='o'&&rheader[i+9]=='n'
	   &&rheader[i+10]==':'&&rheader[i+11]==' '&&rheader[i+12]=='k'&&rheader[i+13]=='e'&&rheader[i+14]=='e'&&rheader[i+15]=='p'
	   &&rheader[i+16]=='-'&&rheader[i+17]=='a'&&rheader[i+18]=='l'&&rheader[i+19]=='i'&&rheader[i+20]=='v'&&rheader[i+21]=='e'
	   &&rheader[i+22]=='\r'&&rheader[i+23]=='\n')
	 {
   	   httpout.write(rheader,0,i);
           httpout.write(cclose,0,19);
	   httpout.write(rheader,i+24,rlength-i-24);
	   i=rlength;
	   isalive=1;
	 }
      }
      
      if(isalive==0)
        httpout.write(rheader,0,rlength);

      while(sizeflag==0)
      {
        http_rbytes = httpin.read(httpbuffer,0,1500);
        clientout.write(httpbuffer,0,http_rbytes);
	for(int i=0;i<http_rbytes;i++)
	{
          if(httpbuffer[i]=='C'&&httpbuffer[i+1]=='o'&&httpbuffer[i+2]=='n'&&httpbuffer[i+3]=='t'&&httpbuffer[i+4]=='e'
	     &&httpbuffer[i+5]=='n'&&httpbuffer[i+6]=='t'&&httpbuffer[i+7]=='-'&&httpbuffer[i+8]=='L'&&httpbuffer[i+9]=='e'
	     &&httpbuffer[i+10]=='n'&&httpbuffer[i+11]=='g'&&httpbuffer[i+12]=='t'&&httpbuffer[i+13]=='h'&&httpbuffer[i+14]==':'
	     &&httpbuffer[i+15]==' ')
	  {
	    for(int j=i+16;httpbuffer[j]!='\r';j++)
	    {
              http_fileSize = http_fileSize*10 + httpbuffer[j]-48;
	    }
	    i=http_rbytes;
	  }
	}
	sizeflag = 1;
      }	
      while((http_rbytes=httpin.read(httpbuffer,0,1500))!=-1)
        clientout.write(httpbuffer,0,http_rbytes);
      
      httpin.close();
      httpout.close();
      https.close();
      clientout.close();
    }
    else
      rescode = 500;
  }
  catch(IOException e)
  { 
    rescode = 500;
  }
  return http_fileSize;
}

int ftp_fetch(Socket client) // NOC - don't change signature
{
     
   int ftp_rbytes=0,ftp_pport=0,ftp_rbytes_psv=0,cmd=1,dir_flag=0,ftp_fileSize=0;
   int pathstart=0,pathend=0,filesizeend=0;
   int[] ftp_psv_ip = new int[6];
   byte[] ftpbuffer = new byte[1024];
   byte[] ftppsvbuffer1 = new byte[1500];
   byte[] ftpfilesize = new byte[10];
   byte[] temp = new byte[5];
   byte[] user = {'U','S','E','R',' ','a','n','o','n','y','m','o','u','s','\r','\n'};
   byte[] pass = {'P','A','S','S',' ','a','@','f','t','p','.','c','o','m','\r','\n'};
   byte[] pasv = {'P','A','S','V','\r','\n'};
   byte[] size = {'S','I','Z','E',' '};
   byte[] fileName = "\r\n".getBytes();
   byte[] cwd = {'C','W','D',' '};
   byte[] retr = {'R','E','T','R',' '};
   byte[] list = {'L','I','S','T','\r','\n'};
   byte[] type = {'T','Y','P','E',' ','I','\r','\n'};
   byte[] clrf = {'\r','\n'};
 
   Socket ftps,ftpspsv = null;
   InputStream ftpis,ftpispsv = null;
   OutputStream ftpout = null,clientout = null;
   try {
   
   ftps = new Socket();
   ftps.connect(new InetSocketAddress(PREFERRED,21));
   ftpis = ftps.getInputStream();
   ftpout = ftps.getOutputStream();
   clientout = client.getOutputStream();
   
   int start=9,flag=0;
   do
   {
      if(rheader[++start]=='/'&&flag==0)
      {
        pathstart=start;
	flag=1;
      }
   }while(rheader[start]!=' ');
   pathend = start - pathstart;
    
   FTP:
   while((ftp_rbytes = ftpis.read(ftpbuffer,0,1024))!=-1)
   {
      byte[] code;
      switch(cmd)
      {
       case 1:
	  code = new byte[]{'2','2','0'};
	  byte[] code1 = {'1','2','0'};
          if(check_ftp_reponse(ftpbuffer,code,ftpis))      
       	  {
	    ftpout.write(user);
	    cmd=2;
	  }
          else if(check_ftp_reponse(ftpbuffer,code1,ftpis))      
	  {
            // wait for 220
	  }
	  else
	  {
	    rescode = 500;
	    break FTP;
	  }
	  break;
       case 2:
         code = new byte[]{'3','3','1'};
         if(check_ftp_reponse(ftpbuffer,code,ftpis))      
       	 { 
	   ftpout.write(pass);
	 }
         else
         {
           code = new byte[]{'2','3','0'};
           if(check_ftp_reponse(ftpbuffer,code,ftpis))      
           {
	     ftpout.write(type);
	     cmd=3;
	   }
           else
	   {
	     rescode = 401;  
             break FTP;
           }
         }
      	 break;
       case 3:
	 code = new byte[]{'2','0','0'};
         if(check_ftp_reponse(ftpbuffer,code,ftpis))      
       	 {
	   ftpout.write(size);
           ftpout.write(rheader,pathstart,pathend);
	   ftpout.write(clrf);
	   cmd=4;
	 }
	 else
	 {
	   rescode = 500;
	   break FTP;
	 }
	 break;
       case 4:
	 code = new byte[]{'2','1','3'};
         if(check_ftp_reponse(ftpbuffer,code,ftpis))      
       	 {
	   int i=4;
	   for(int j=-1;ftpbuffer[i]!='\r';i++)
           {
	     ftpfilesize[++j]=ftpbuffer[i];
           }		   
           filesizeend=i-4;
           ftpout.write(pasv);
	   cmd=5;
	 }
	 else
         {
	   rescode = 404;  
	   break FTP;
	 }
         break;
       case 5:
	 code = new byte[]{'2','2','7'};
         if(check_ftp_reponse(ftpbuffer,code,ftpis))      
       	 {
	   for(int i=ftp_rbytes-3,j=-1,index=-1;i>=0;i--)
	   {
	     if(ftpbuffer[i]==','||ftpbuffer[i]=='(')
             {
		int temp1=0;
		for(int k=j;k>=0;k--)
		{
		  temp1=temp1*10+temp[k]-48;
		}
		ftp_psv_ip[++index]=temp1;
		j=-1;
	     }
	     if(ftpbuffer[i]=='(')
	        break;
	     else if(ftpbuffer[i]!=','&&ftpbuffer[i]>=48&&ftpbuffer[i]<=57)
	     {
		temp[++j]=ftpbuffer[i];
             }
           }
	   ftp_pport=ftp_psv_ip[1]*256+ftp_psv_ip[0];
	   ftpspsv = new Socket();
	   ftpspsv.connect(new InetSocketAddress(PREFERRED,ftp_pport));
           ftpispsv = ftpspsv.getInputStream();	 
	   ftpout.write(retr);
	   ftpout.write(rheader,pathstart,pathend);
	   ftpout.write(clrf);
	   cmd=6;
	 }
	 else
	 {
	   rescode = 500;
           break FTP;
	 }
         break;
       case 6:
	 code = new byte[]{'1','5','0'};
	 if(check_ftp_reponse(ftpbuffer,code,ftpis))      
       	 {
	   byte[] respcode = {'H','T','T','P','/','1','.','1',' ','2','0','0',' ','O','K'};
	   byte[] conlength = {'C','o','n','t','e','n','t','-','L','e','n','g','t','h',':',' '};
	   byte[] server = {'S','e','r','v','e','r',':',' ','A','p','a','c','h','e','/','2','.','4','.','6'};
	   clientout.write(respcode);clientout.write(clrf);
	   clientout.write(conlength);
           clientout.write(ftpfilesize,0,filesizeend);clientout.write(clrf);
	   clientout.write(server);clientout.write(clrf);
	   clientout.write(clrf);
	   while((ftp_rbytes_psv = ftpispsv.read(ftppsvbuffer1,0,1500))!=-1)
	   {
	     clientout.write(ftppsvbuffer1,0,ftp_rbytes_psv);  
	     ftp_fileSize += ftp_rbytes_psv;
           }
	   cmd=7;
	 }
	 else
	 {
	   rescode = 500;
	   break FTP;
	 }
         break;
       case 7:
	 code = new byte[]{'2','2','6'};
	 if(!check_ftp_reponse(ftpbuffer,code,ftpis))      
       	   rescode = 500;
	 ftpispsv.close();
	 ftpspsv.close();
	 ftpis.close();
	 ftpout.close();	
         ftps.close();
	 clientout.close();
	 break FTP;
      }
  }  
  }
  catch(Exception e)
  {
    e.printStackTrace();  
    rescode = 500;
  }
  return ftp_fileSize;
}

int serv_resp(Socket client)
{
       // Print Server Response
    if(rescode==200)
    {
      if(rheader[4]=='/')
      
        System.out.println("REQ: " + byte2str(HOST,0,hlength) + byte2str(rheader,urlstart,urlend) +
	" (" + filesize + " bytes transferred)");
      else
        System.out.println("REQ: " + byte2str(rheader,urlstart,urlend) + " (" + filesize + " bytes transferred)");
    
    }
    else
    {
      if(rheader[4]=='/')
         System.out.println("REQ: " + byte2str(HOST,0,hlength) + byte2str(rheader,urlstart,urlend) + "(ERROR) ");
      else
         System.out.println("REQ: " + byte2str(rheader,urlstart,urlend) + "(ERROR) ");
    }
    // Write Response Header to Outputstream in case of error

    if(rescode!=200)
    {
      
      byte[] clrf = {'\r','\n'};
      byte[] http = {'H','T','T','P','/','1','.','1',' '};
      byte[] conlength = {'C','o','n','t','e','n','t','-','L','e','n','g','t','h',':',' '};
      byte[] server = {'S','e','r','v','e','r',':',' ','A','p','a','c','h','e','/','2','.','4','.','6'};
      byte[] contype = {'C','o','n','t','e','n','t','-','T','y','p','e',':',' ','t','e','x','t','/','h','t','m','l',';',
                     ' ','c','h','a','r','s','e','t','=','u','t','f','-','8'};
      byte[] h501 = {'5','0','1',' ','N','o','t',' ','I','m','p','l','e','m','e','n','t','e','d'};
      byte[] h501len = {'1','2','7'};
      byte[] h505 = {'5','0','5',' ','H','T','T','P',' ','V','e','r','s','i','o','n',' ','N','o','t',' ',
                     'S','u','p','p','o','r','t','e','d'};
      byte[] h505len = {'1','3','1'};
      byte[] h502 = {'5','0','2',' ','B','a','d',' ','G','a','t','e','w','a','y'};
      byte[] h502len = {'1','1','6'};
      byte[] h400 = {'4','0','0',' ','B','a','d',' ','R','e','q','u','e','s','t'};
      byte[] h400len = {'1','1','6'};
      byte[] h401 = {'4','0','1',' ','U','n','a','u','t','h','o','r','i','z','e','d'};
      byte[] h401len = {'1','1','8'};
      byte[] h404 = {'4','0','4',' ','N','o','t',' ','F','o','u','n','d'};
      byte[] h404len = {'1','1','2'};
      byte[] h500 = {'5','0','0',' ','I','n','t','e','r','n','a','l',' ','S','e','r','v','e','r',' ',
                     'E','r','r','o','r'};
      byte[] h500len = {'1','3','6'};
      byte[] htmlopen = {'<','h','t','m','l','>','\n','<','h','e','a','d','>','\n',
                         '<','/','h','e','a','d','>','\n','<','b','o','d','y','>','\n'};
      byte[] htmlclose = {'<','/','b','o','d','y','>','\n','<','/','h','t','m','l','>'};

      try { 

      OutputStream out = client.getOutputStream();
      out.write(http);

      if(rescode==501)
      { 
        out.write(h501);out.write(clrf);
        out.write(conlength);
	out.write(h501len);out.write(clrf);
      }  
      else if(rescode==505)
      {
        out.write(h505);out.write(clrf);
        out.write(conlength);
	out.write(h505len);out.write(clrf);
      }
      else if(rescode==502)
      {
        out.write(h502);out.write(clrf);
        out.write(conlength);
	out.write(h502len);out.write(clrf);
      }
      else if(rescode==500)
      {
        out.write(h500);out.write(clrf);
        out.write(conlength);
	out.write(h500len);out.write(clrf);
      }
      else if(rescode==401)
      {
        out.write(h401);out.write(clrf);
        out.write(conlength);
	out.write(h401len);out.write(clrf);
      }
      else if(rescode==404)
      {
        out.write(h404);out.write(clrf);
        out.write(conlength);
	out.write(h404len);out.write(clrf);
      }
      else
      {
        out.write(h400);out.write(clrf);
        out.write(conlength);
	out.write(h400len);out.write(clrf);
      }
       
      out.write(server);out.write(clrf);
      out.write(contype);out.write(clrf);
      out.write(clrf);
        
      out.write(htmlopen);
    	  
      if(rescode==501)
      {
        out.write(h501);out.write('\n');
      }
      else if(rescode==505)
      {
        out.write(h505);out.write('\n');
      }
      else if(rescode==502)
      {
        out.write(h502);out.write('\n');
      }
      else if(rescode==500)
      {
        out.write(h500);out.write('\n');
      }
      else if(rescode==401)
      {
        out.write(h401);out.write('\n');
      }
      else if(rescode==404)
      { 
        out.write(h404);out.write('\n');
      }
      else
        out.write(h400);out.write('\n');
       
      
      out.write(htmlclose);
      out.flush();
      out.close();
      }
      catch(IOException e)
      { 
        e.printStackTrace();
      }
    }
  return 0;
}
 
int run(int X)  // NOC - do not change the signature for run()
{
  ServerSocket s0 = null; // NOC - this is the listening socket
  Socket s1 = null; // NOC - this is the accept-ed socket i.e. client
 // byte[] b0 = new byte[1024];  // ADC - general purpose buffer
 
  InputStream in = null;
  InetAddress local = null;
  
  int reqcount = 1,rbytes;

  try
  {
    s0 = new ServerSocket();
    s0.bind(new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),PORT),X);
    System.out.println("Apache Listening on socket " + PORT + "\n");
    while (true)
    {  
      s1 = s0.accept();
      System.out.println("(" + reqcount++ + ") Incoming client connection from [" 
      + s1.getRemoteSocketAddress() + "] to me [" + s0.getLocalSocketAddress() + "]");
      in = s1.getInputStream();
      rbytes=0;rlength=0;rescode=200;hlength=0;urlstart=0;urlend=0;filesize=0;
      Term1:
      do
      { 		
        rbytes = in.read(rheader,rlength,65535-rlength);
	rlength = rlength + rbytes;
	if((rlength==65535)||(rbytes<4))
	  break Term1;
      }while(!((rheader[rlength-4]==13)&&(rheader[rlength-3]==10)&&(rheader[rlength-2]==13)&&(rheader[rlength-1]==10)));
      if((rlength<=26)||(rlength==65535&&rheader[rlength-1]!='\n'))
       	rescode=400;
      else
      {
        rescode=parse(rheader);
        if(rescode==200)
	{
	  rescode=dns(0);
          if(rescode==200)
	  {
	    if(check_http(rheader))
	      filesize = http_fetch(s1);
	    else
	      filesize = ftp_fetch(s1);		   
	  }
	}   
      }
      serv_resp(s1);
      in.close();
      s1.close();      
    }
  }
  catch(Exception e)
  {
     e.printStackTrace();
  }  

  return 1;
}

/* ------------- your own methods below this line ONLY ----- */
String byte2str(byte []b, int i, int j)
{
  String s = "";
  for(int k=i;k<j;k++)
  {
    char c=(char)b[k];
    s=s+c;
  }
  return s;
}

void setHost(byte[] host,int offset,int length)
{
  int j=0;
  for(int i=offset;i<=length;i++,j++)
      HOST[j]=host[i];
  hlength=j;
}
boolean check_ftp_reponse(byte[] resp, byte[] code,InputStream ftpis)
{
  int ftp_rbytes,exit=0;
  if(resp[0]==code[0]&&resp[1]==code[1]&&resp[2]==code[2])
  {
    if(resp[3]!=' ')
    { 
      do
      {
        try
	{
	  ftp_rbytes = ftpis.read(resp,0,1024);
	  for(int i=0;i<ftp_rbytes;i++)
	  {
	    if(resp[i]=='\r'&&resp[i+1]=='\n'&&resp[i+5]==' ')
	       exit=1;
	  }
        }
	catch(Exception e)
	{
	  exit=1;
	}
      }while(exit==0);
    }
    return true;
  }
  else
    return false;
}   
boolean check_http(byte[] buffer)
{
  if(buffer[4]=='h'||buffer[4]=='/')
    return true;
  else
    return false;		
}
}// class Apache
