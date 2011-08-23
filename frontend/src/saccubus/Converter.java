package saccubus;

import javax.swing.JLabel;
import saccubus.net.NicoClient;
import java.io.*;

import saccubus.conv.CombineXML;
import saccubus.conv.ConvertToVideoHook;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;

import saccubus.util.Cws2Fws;
import saccubus.ConvertingSetting;

/**
 * <p>タイトル: さきゅばす</p>
 *
 * <p>説明: ニコニコ動画の動画をコメントつきで保存</p>
 *
 * <p>著作権: Copyright (c) 2007 PSI</p>
 *
 * <p>会社名: </p>
 *
 * @author 未入力
 * @version 1.0
 */
public class Converter extends Thread {
	private ConvertingSetting Setting;
	private String Tag;
	private String VideoID;
	private String VideoTitle;
	private String Time;
	private JLabel Status;
	private final ConvertStopFlag StopFlag;
	private static final String TMP_COMMENT = "./vhook.tmp";
	private static final String TMP_OWNERCOMMENT = "./vhookowner.tmp";
	private static final String VIDEO_URL_PARSER = "http://www.nicovideo.jp/watch/";
	public static final String OWNER_EXT = "[Owner].xml";	// 投稿者コメントサフィックス
	private static final String TMP_COMBINED_XML = "./tmp_comment.xml";
	private String OtherVideo;

	public Converter(String url, String time, ConvertingSetting setting,
			JLabel status, ConvertStopFlag flag) {
		url = url.trim();
		if(url.startsWith(VIDEO_URL_PARSER)){
			int index = url.indexOf('?',VIDEO_URL_PARSER.length());
			if(index >= 0){
				Tag = url.substring(VIDEO_URL_PARSER.length(),index);
			}else{
				Tag = url.substring(VIDEO_URL_PARSER.length());
			}
		}else{
			Tag = url;
		}
		VideoID = "[" + Tag + "]";
		Time = time;
		Setting = setting;
		Status = status;
		StopFlag = flag;
	}

	private File VideoFile = null;
	private File CommentFile = null;
	private File OwnerCommentFile = null;
	private File ConvertedVideoFile = null;
	private File CommentMiddleFile = null;
	private File OwnerMiddleFile = null;

	public File getVideoFile() {
		return VideoFile;
	}

	private void sendtext(String text){
		Status.setText(text);
	}
	private boolean isSaveConverted(){
		return Setting.isSaveConverted();
	}
	private boolean isSaveVideo(){
		return Setting.isSaveVideo();
	}
	private boolean isSaveComment(){
		return Setting.isSaveComment();
	}
	private boolean isSaveOwnerComment(){
		return Setting.isSaveOwnerComment();
	}
	private boolean isConvertWithComment(){
		return Setting.isConvertWithComment();
	}
	private boolean isConvertWithOwnerComment(){
		return Setting.isConvertWithOwnerComment();
	}
	private boolean isVideoFixFileName(){
		return Setting.isVideoFixFileName();
	}
	private boolean isCommentFixFileName(){
		return Setting.isCommentFixFileName();
	}
	private String getProxy(){
		return Setting.getProxy();
	}
	private int getProxyPort(){
		return Setting.getProxyPort();
	}
	private String getMailAddress(){
		return Setting.getMailAddress();
	}
	private String getPassword(){
		return Setting.getPassword();
	}
	private boolean isDeleteVideoAfterConverting(){
		return Setting.isDeleteVideoAfterConverting();
	}
	private boolean isDeleteCommentAfterConverting(){
		return Setting.isDeleteCommentAfterConverting();
	}
	private boolean useProxy(){
		return Setting.useProxy();
	}

	private boolean checkOK() {
		sendtext("チェックしています");
		if (!isSaveConverted() && !isSaveVideo()
			&& !isSaveComment() && !isSaveOwnerComment()){
			sendtext("何もすることがありません");
			return false;
		}
		if (isSaveConverted()) {
			File a = new File(Setting.getFFmpegPath());
			if (!a.canRead()) {
				sendtext("FFmpegが見つかりません。");
				return false;
			}
			if (Setting.getVhookPath().indexOf(' ') >= 0) {
				sendtext("すいません。現在vhookライブラリには半角空白は使えません。");
				return false;
			}
			a = new File(Setting.getVhookPath());
			if (!a.canRead()) {
				sendtext("Vhookライブラリが見つかりません。");
				return false;
			}
			a = new File(Setting.getFontPath());
			if (!a.canRead()) {
				sendtext("フォントが見つかりません。");
				return false;
			}
			if (!detectOption()) {
				sendtext("変換オプションファイルの読み込みに失敗しました。");
				return false;
			}
		} else {
			if (isDeleteVideoAfterConverting()) {
				sendtext("変換しないのに、動画削除しちゃって良いんですか？");
				return false;
			}
			if (isDeleteCommentAfterConverting()) {
				sendtext("変換しないのに、コメント削除しちゃって良いんですか？");
				return false;
			}
		}
		if (isSaveVideo() ||
				isSaveComment() || isSaveOwnerComment()) {
			if (getMailAddress() == null
					|| getMailAddress().equals("")
					|| getPassword() == null
					|| getPassword().equals("")) {
				sendtext("メールアドレスかパスワードが空白です。");
				return false;
			}
			if (useProxy()
				&& (   getProxy() == null || getProxy().length() <= 0
				    || getProxyPort() < 0 || getProxyPort() > 65535   )){
				sendtext("プロキシの設定が不正です。");
				return false;
			}
		}
		sendtext("チェック終了");
		return true;
	}

	private NicoClient getNicoClient() {
		if (isSaveVideo() || isSaveComment() || isSaveOwnerComment()) {
			String proxy = null;
			int proxy_port = -1;
			if (useProxy()) {
				proxy = getProxy();
				proxy_port = getProxyPort();
			}
			sendtext("ログイン中");
			NicoClient client = new NicoClient(getMailAddress(),
					getPassword(), StopFlag, proxy, proxy_port);
			if (!client.isLoggedIn()) {
				sendtext("ログインに失敗");
			} else {
				sendtext("ログイン成功");
			}
			return client;
		} else {
			return null;
		}
	}

	private boolean saveVideo(NicoClient client) {
		File folder = Setting.getVideoFixFileNameFolder();
		sendtext("動画の保存");
		/*動画の保存*/
		if (isSaveVideo()) {
			if (isVideoFixFileName()) {
				folder.mkdir();
				VideoFile = new File(folder,
						VideoID + VideoTitle + ".flv");
			} else {
				VideoFile = Setting.getVideoFile();
			}
			sendtext("動画のダウンロード開始中");
			if (client == null){
				sendtext("ログインしてないのに動画の保存になりました");
				return false;
			}
			VideoFile = client.getVideo(VideoFile, Status);
			if (stopFlagReturn()) {
				return false;
			}
			if (VideoFile == null) {
				sendtext("動画のダウンロードに失敗");
				return false;
			}
		} else {
			if (isSaveConverted()) {
				if (isVideoFixFileName()) {
					String videoFilename;
					if((videoFilename = detectTitleFromVideo(folder)) == null){
						if (OtherVideo == null){
							sendtext("動画ファイルがフォルダに存在しません。");
						} else {
							sendtext("動画ファイルが.flvでありません：" + OtherVideo);
						}
						return false;
					}
					VideoFile = new File(folder, videoFilename);
					if (!VideoFile.canRead()) {
						sendtext("動画ファイルが読み込めません。");
						return false;
					}
				} else {
					VideoFile = Setting.getVideoFile();
					if (!VideoFile.exists()) {
						sendtext("動画ファイルが存在しません。");
						return false;
					}
				}
			}
		}
		sendtext("動画の保存を終了");
		return true;
	}

	private boolean saveComment(NicoClient client) {
		sendtext("コメントの保存");
		File folder = Setting.getCommentFixFileNameFolder();
		if (isSaveComment()) {
			if (isCommentFixFileName()) {
				folder.mkdir();
				String ext = ".xml";
				if (Setting.isAddTimeStamp()) {
					MyDateFormat mdf = new MyDateFormat();
					if(Time == null || Time.equals("")){
						ext = "[" + mdf.formatNow() + "]" + ext;
					} else if (mdf.makeTime(Time)){
						ext = "[" + mdf.formatDate() + "]" + ext;
					} else {
						ext = "[" + Time + "]" + ext;
					}
				}
				CommentFile = new File(folder,VideoID + VideoTitle + ext);
			} else {
				CommentFile = Setting.getCommentFile();
			}
			if (client == null){
				sendtext("ログインしてないのにコメントの保存になりました");
				return false;
			}
			String back_comment = Setting.getBackComment();
			if (Setting.isFixCommentNum()) {
				back_comment = client
						.getBackCommentFromLength(back_comment);
			}
			sendtext("コメントのダウンロード開始中");
			CommentFile = client.getComment(CommentFile, Status,
					back_comment);
			if (stopFlagReturn()) {
				return false;
			}
			if (CommentFile == null) {
				sendtext("コメントのダウンロードに失敗");
				return false;
			}
		}
		sendtext("コメントの保存終了");
		return true;
	}

	private boolean saveOwnerComment(NicoClient client){
		sendtext("投稿者コメントの保存");
		File folder = Setting.getCommentFixFileNameFolder();
		if (isSaveOwnerComment()) {
			if (isCommentFixFileName()) {
				folder.mkdir();
				OwnerCommentFile = new File(folder,
					VideoID + VideoTitle + OWNER_EXT);
			} else {
				OwnerCommentFile = Setting.getOwnerCommentFile();
			}
			sendtext("投稿者コメントのダウンロード開始中");
			if (client == null){
				sendtext("ログインしてないのに投稿者コメントの保存になりました");
				return false;
			}
			OwnerCommentFile = client.getOwnerComment(OwnerCommentFile, Status);
			if (stopFlagReturn()) {
				return false;
			}
			if (OwnerCommentFile == null) {
				sendtext("投稿者コメントのダウンロードに失敗");
				return false;
			}
		}
		sendtext("投稿者コメントの保存終了");
		return true;
	}

	private boolean convertComment(){
		sendtext("コメントの中間ファイルへの変換中");
		File folder = Setting.getCommentFixFileNameFolder();
		if (isConvertWithComment()) {
			if (Setting.isAddTimeStamp() && isCommentFixFileName()) {
				// 複数のコメントファイル（過去ログ）があるかも
				ArrayList<String> pathlist = detectFilelistFromComment(folder);
				if (pathlist == null || pathlist.isEmpty()){
					sendtext(Tag + ": コメントファイル・過去ログが存在しません。");
					return false;
				}
				// VideoTitle は見つかった。
				if (pathlist.size() > 1) {
					ArrayList<File> filelist = new ArrayList<File>();
					for(int i = 0; i < pathlist.size(); i++){
						filelist.add(new File(folder, pathlist.get(i)));
					}
					CommentFile = new File(TMP_COMBINED_XML);
					if (!CombineXML.combineXML(filelist, CommentFile)){
						sendtext("コメントファイルが結合出来ませんでした（バグ？）");
						return false;
					}
				} else {
					// コメントファイルはひとつだけ見つかった
					File comfile = new File(folder,pathlist.get(0));
					if (isSaveComment()
							&& comfile.getPath() != CommentFile.getPath()){
						sendtext("保存したコメントファイルが見つかりません。");
						return false;
					}
				}
			}
			if (!isSaveComment()) {
				if (isCommentFixFileName()) {
					if (!Setting.isAddTimeStamp()){
						// コメントファイルはひとつ
						String commentfilename = detectTitleFromComment(folder);
						if(commentfilename == null){
							sendtext("コメントファイルがフォルダに存在しません。");
							return false;
						}
						// VideoTitle は見つかった。
						CommentFile = new File(folder, commentfilename);
						if (!CommentFile.canRead()) {
							sendtext("コメントファイルが読み込めません。");
							return false;
						}
					} else {
						// 処理済み
					}
				} else {
					CommentFile = Setting.getCommentFile();
					if (!CommentFile.exists()) {
						sendtext("コメントファイルが存在しません。");
						return false;
					}
				}
			}
			CommentMiddleFile = convertToCommentMiddle(
					CommentFile, TMP_COMMENT);
			if (CommentMiddleFile == null){
				sendtext("コメント変換に失敗。おそらく正規表現の間違い？");
				return false;
			}
		}
		return true;
	}

	private boolean convertOwnerComment(){
		sendtext("投稿者コメントの中間ファイルへの変換中");
		File folder = Setting.getCommentFixFileNameFolder();
		if (isConvertWithOwnerComment()){
			if (!isSaveOwnerComment()) {
				if (isCommentFixFileName()) {
					String ownerfilename = detectTitleFromOwnerComment(folder);
					if(ownerfilename == null){
						sendtext("投稿者コメントファイルがフォルダに存在しません。");
						return false;
					}
					// VideoTitle は見つかった。
					OwnerCommentFile = new File(folder, ownerfilename);
					if (!OwnerCommentFile.canRead()) {
						sendtext("投稿者コメントファイルが読み込めません。");
						return false;
					}
				} else {
					OwnerCommentFile = Setting.getOwnerCommentFile();
					if (!OwnerCommentFile.exists()) {
						sendtext("投稿者コメントファイルが存在しません。");
						return false;
					}
				}
			}
			OwnerMiddleFile = convertToCommentMiddle(
					OwnerCommentFile, TMP_OWNERCOMMENT);
			if (OwnerMiddleFile == null){
				sendtext("投稿者コメント変換に失敗。おそらく正規表現の間違い？");
				return false;
			}
		}
		return true;
	}

	private void deleteCommentFile(){
		CommentFile.delete();
	}

	private File convertToCommentMiddle(File commentfile,
			String outpath) {
		File middlefile = new File(outpath);
		if (!ConvertToVideoHook.convert
			(commentfile, middlefile,
			 Setting.getNG_ID(), Setting.getNG_Word())){
			return null;
		}
		return middlefile;
	}

	private boolean convertVideo() {
		sendtext("動画の変換を開始");
		/*ビデオ名の確定*/
		File folder = Setting.getConvFixFileNameFolder();
		if (Setting.isConvFixFileName()) {
			folder.mkdir();
			String conv_name = VideoTitle;
			if (!Setting.isNotAddVideoID_Conv()) {//付加するなら
				if (conv_name == null){
					conv_name = VideoID;
				} else {
					conv_name = VideoID + conv_name;
				}
			}
			if (conv_name == null || conv_name.isEmpty()) {
				sendtext("変換後のビデオファイル名が確定できません。");
				return false;
			}
			ConvertedVideoFile = new File(folder, conv_name + ExtOption);
		} else {
			String filename = Setting.getConvertedVideoFile().getPath();
			if (!filename.endsWith(ExtOption)) {
				filename = filename.substring(0, filename.lastIndexOf('.'))
						+ ExtOption;
				ConvertedVideoFile = new File(filename);
			} else {
				ConvertedVideoFile = Setting.getConvertedVideoFile();
			}
		}
		if (ConvertedVideoFile.getPath().equals(VideoFile.getPath())){
			sendtext("変換後のファイル名が変換前の動画ファイルと同じです");
			return false;
		}
		int code = converting_video(TMP_COMMENT,TMP_OWNERCOMMENT);
		if (code == 0) {
			sendtext("変換が正常に終了しました。");
			return true;
		} else if (code == CODE_CONVERTING_ABORTED) { /*中断*/

		} else {
			sendtext("変換エラー：" + LastError);
		}
		return false;
	}

	@Override
	public void run() {
		try {
			if (!checkOK()) {
				return;
			}
			NicoClient client = getNicoClient();
			if (client != null){
				if (!client.isLoggedIn()){
					return;
				}
				if (!client.getVideoInfo(Tag, Time)) {
					sendtext(Tag + "の情報の取得に失敗");
					return;
				}
				sendtext(Tag + "の情報の取得に成功");
				if (stopFlagReturn()) {
					return;
				}
				VideoTitle = client.getVideoTitle();
			}

			if (!saveVideo(client) || stopFlagReturn()) {
				return;
			}

			if (!saveComment(client) || stopFlagReturn()){
				return;
			}

			if (!saveOwnerComment(client) || stopFlagReturn()) {
				return;
			}

			if (!isSaveConverted()) {
				sendtext("動画・コメントを保存し、変換は行いませんでした。");
				return;
			}

			if (!convertComment() || stopFlagReturn()) {
				return;
			}

			if (!convertOwnerComment() || stopFlagReturn()){
				return;
			}

			if (convertVideo()) {
				if (isDeleteCommentAfterConverting()
					&& CommentFile != null) {
					deleteCommentFile();
					// OwnerCommentFile.delete();
				}
				if (isDeleteVideoAfterConverting()
					&& VideoFile != null) {
					VideoFile.delete();
				}
			}
		} finally {
			StopFlag.finished();
			if (CommentMiddleFile != null) {
				//  デバッグのためコメントファイルを残しておく
				// CommentMiddleFile.delete();
			}
			if (OwnerMiddleFile != null){
				//	投稿者コメントは削除しない。
				// OwnerMiddleFile.delete();
			}
		}
	}

	String LastError;

	private static final int CODE_CONVERTING_ABORTED = 100;

	private int converting_video(String vhook_path, String vhookowner_path) {
		Process process = null;
		BufferedReader ebr = null;
		File fwsFile = Cws2Fws.createFws(VideoFile);

		StringBuffer sb = new StringBuffer();
		sb.append("\"");
		sb.append(Setting.getFFmpegPath().replace("\\", "/"));
		sb.append("\"");
		sb.append(" -y ");
		sb.append(MainOption);
		sb.append(" ");
		sb.append(InOption);
		sb.append(" -i ");
		if (fwsFile == null) {
			sb.append("\"");
			sb.append(VideoFile.getPath().replace("\\", "/"));
			sb.append("\"");
		} else {
			sb.append(fwsFile.getPath().replace("\\", "/"));
		}
		sb.append(" ");
		sb.append(OutOption);
		sb.append(" \"");
		sb.append(ConvertedVideoFile.getPath().replace("\\", "/"));
		sb.append("\"");
		if (!Setting.isVhookDisabled()) {
			if(!addVhookSetting(sb, vhook_path, vhookowner_path)){
				return -1;
			}
		}
		String cmd = sb.substring(0);
		System.out.println("arg:" + cmd);
		try {
			System.out.println("\n\n----\nProcessing FFmpeg...\n----\n\n");
			process = Runtime.getRuntime().exec(cmd);
			ebr = new BufferedReader(new InputStreamReader(
					process.getErrorStream()));
			String e;
			while ((e = ebr.readLine()) != null) {
				LastError = e;
				if (LastError.startsWith("frame=")) { //
					sendtext(LastError);
				} else if(!LastError.endsWith("No accelerated colorspace conversion found")){
					System.out.println(e);
				}
				if (stopFlagReturn()) {
					process.destroy();
					return CODE_CONVERTING_ABORTED;
				}
			}
			process.waitFor();
			return process.exitValue();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
			return -1;
		} catch (IOException ex) {
			ex.printStackTrace();
			return -1;
		} finally {
			try {
				process.getErrorStream().close();
				ebr.close();
				if (fwsFile != null) {
				//	fwsFile.delete();	// For DEBUG
				}
			} catch(Exception ex){
        		ex.printStackTrace();
			}
		}
	}

	private boolean addVhookSetting(StringBuffer sb,
			String vhook_path, String vhookowner_path) {
		try {
			sb.append(" -vfilters \"vhext=");
			sb.append(Setting.getVhookPath().replace("\\", "/"));
			sb.append("|");
			if(CommentMiddleFile!=null){
				sb.append("--data-user:");
				sb.append(URLEncoder.encode(
					vhook_path.replace("\\","/"),"Shift_JIS"));
				sb.append("|");
			}
			if(OwnerMiddleFile!=null){
				sb.append("--data-owner:");
				sb.append(URLEncoder.encode(
					vhookowner_path.replace("\\","/"),"Shift_JIS"));
				sb.append("|");
			}
			sb.append("--font:");
			sb.append(URLEncoder.encode(
					Setting.getFontPath().replace("\\","/"), "Shift_JIS"));
			sb.append("|");
			sb.append("--font-index:");
			sb.append(Setting.getFontIndex());
			sb.append("|");
			sb.append("--show-user:");
			sb.append(Setting.getVideoShowNum());
			sb.append("|");
			sb.append("--shadow:");
			sb.append(Setting.getShadowIndex());
			sb.append("|");
			if (Setting.isVhook_ShowConvertingVideo()) {
				sb.append("--enable-show-video");
				sb.append("|");
			}
			if (Setting.isFixFontSize()) {
				sb.append("--enable-fix-font-size");
				sb.append("|");
			}
			if (Setting.isOpaqueComment()) {
				sb.append("--enable-opaque-comment");
			}
			sb.append("\"");
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}
	}

	/*
	 private static void addArrayToList(ArrayList<String> list,String array[]){
	 for(int i=0;i<array.length;i++){
	 list.add(array[i]);
	 }
	 }
	 private static String escape(String str){
	 byte[] buff = null;
	 try {
	 buff = str.getBytes("Shift_JIS");
	 } catch (UnsupportedEncodingException e) {
	 e.printStackTrace();
	 }
	 int cnt = 0;
	 for(int i=0;i<buff.length;i++){
	 if(buff[i] == '\\' || buff[i] == '{' || buff[i] == '}'){
	 cnt++;
	 }
	 cnt++;
	 }
	 byte[] obuff = new byte[cnt];
	 cnt = 0;
	 for(int i=0;i<buff.length;i++){
	 if(buff[i] == '\\' || buff[i] == '{' || buff[i] == '}'){
	 obuff[cnt] = '\\';
	 cnt++;
	 }
	 obuff[cnt] = buff[i];
	 cnt++;
	 }
	 try {
	 String out = new String(obuff,"Shift_JIS");
	 return out;
	 } catch (UnsupportedEncodingException e) {
	 e.printStackTrace();
	 }
	 return "";
	 }
	 */
	public boolean isConverted() {
		return StopFlag.isConverted();
	}

	private boolean stopFlagReturn() {
		if (StopFlag.needStop()) {
			sendtext("中止しました。");
			return true;
		}
		return false;
	}

	public ConvertStopFlag getStopFlag() {
		return this.StopFlag;
	}

	private String ExtOption;

	private String InOption;

	private String OutOption;

	private String MainOption;

	private boolean detectOption() {
		boolean ret;
		if (Setting.getOptionFile() != null) {
			try {
				Properties prop = new Properties();
				prop.loadFromXML(new FileInputStream(Setting.getOptionFile()));
				ExtOption = prop.getProperty("EXT", null);
				InOption = prop.getProperty("IN", null);
				OutOption = prop.getProperty("OUT", null);
				MainOption = prop.getProperty("MAIN", null);
				if (ExtOption != null && InOption != null && OutOption != null
						&& MainOption != null) {
					ret = true;
				} else {
					ret = false;
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				ret = false;
			}
		} else {
			ExtOption = Setting.getCmdLineOptionExt();
			InOption = Setting.getCmdLineOptionIn();
			OutOption = Setting.getCmdLineOptionOut();
			MainOption = Setting.getCmdLineOptionMain();
			ret = true;
		}
		//オプションに拡張子を含んでしまった場合にも対応☆
		if(!ExtOption.startsWith(".")){
			ExtOption = "."+ExtOption;
		}
		return ret;
	}
	class VideoIDFilter implements FilenameFilter {
		private final String VideoTag;
		public VideoIDFilter(String videoTag){
			VideoTag = videoTag;
		}
		@Override
		public boolean accept(File dir, String name) {
			if (name.indexOf(VideoTag) >= 0){
				return true;
			}
			return false;
		}
	}

	private String detectTitleFromVideo(File dir){
		String list[] = dir.list(new VideoIDFilter(VideoID));
		if(list == null){
			return null;
		}
		for (int i = 0; i < list.length; i++) {
			if (list[i].startsWith(VideoID)) {
				String path = list[i];
				if(!path.endsWith(".flv")){
					OtherVideo = path;
					continue;
				}
				if (VideoTitle != null){
					return path;
				}
				VideoTitle = path.substring(VideoID.length());
				int lastindex = VideoTitle.lastIndexOf('.');
				if (lastindex >= 0) {
					VideoTitle = VideoTitle.substring(0,lastindex);
				}
				return path;
			}
		}
		return null;
	}

	private String detectTitleFromComment(File dir){
		String list[] = dir.list(new VideoIDFilter(VideoID));
		if(list == null){ return null; }
		for (int i = 0; i < list.length; i++) {
			String path = list[i];
			if (!path.endsWith(".xml") || path.endsWith(OWNER_EXT)){
				continue;
			}
			if (VideoTitle == null){
				int fromindex = VideoID.length();
					// 何故ならば VideoID が頭に付くから
				int index = path.lastIndexOf("[");
					//過去ログは[YYYY/MM/DD_HH:MM:SS]が最後に付く
				if (index >= fromindex){
					VideoTitle = path.substring(fromindex, index);
				} else {
					VideoTitle = path.substring(
							fromindex, path.lastIndexOf("."));
				}
			}
			return path;
		}
		return null;
	}

	private static final String TCOMMENT_EXT =".txml";
	private String detectTitleFromOwnerComment(File dir){
		String list[] = dir.list(new VideoIDFilter(VideoID));
		if(list == null){ return null; }
		for (int i = 0; i < list.length; i++) {
			String path = list[i];
			String ext;
			if (path.endsWith(OWNER_EXT)){
				ext = OWNER_EXT;
			} else if (path.endsWith(TCOMMENT_EXT)) {
				ext = TCOMMENT_EXT;
			} else {
				continue;
			}
			if (VideoTitle == null){
				VideoTitle = path.substring(VideoID.length(),
						path.lastIndexOf(ext));
			}
			return path;
		}
		return null;
	}

	private ArrayList<String> detectFilelistFromComment(File dir){
		String list[] = dir.list(new VideoIDFilter(VideoID));
		if (list == null) { return null; }
		ArrayList<String> filelist = new ArrayList<String>();
		for (int i = 0; i < list.length; i++){
			String path = list[i];
			if (!path.endsWith(".xml") || path.endsWith(OWNER_EXT)){
				continue;
			}
			if (VideoTitle == null){
				int fromindex = VideoID.length();
					// 何故ならば VideoID が頭に付くから
				int index = path.lastIndexOf("[");
					//過去ログは[YYYY/MM/DD_HH:MM:SS]が最後に付く
				if (index >= fromindex){
					VideoTitle = path.substring(fromindex, index);
				} else {
					VideoTitle = path.substring(
							fromindex, path.lastIndexOf(".xml"));
				}
			}
			filelist.add(path);
		}
		if (filelist.isEmpty()){
			return null;
		}
		return filelist;
	}

	/*
	 * 	投稿者コメントに関するファイル名タグと拡張子を扱う
	 * 		�@ NicoBrowser拡張1.4.4の場合
	 * 			ユーザコメント = VideiID + VideoTitle + ".xml"
	 * 			投稿者コメント = VideoID + VideoTitle + ".txml"
	 * 		�A今回のさきゅばす
	 * 			ユーザコメント = VideoID + VideoTitle + ".xml"
	 * 			投稿者コメント = VideoID + VideoTitle + "{Owner].xml"
	 * 		�BNNDDなど
	 * 			ユーザコメント = VideoTitle + VideoID + ".xml"
	 * 			投稿者コメント = VideoTitle + VideoID + "{Owner].xml"
	 * 		�CNicoPlayerなど
	 * 			ユーザコメント = VideoTitle + "(" + Tag + ")" + ".xml"
	 * 			過去ログ       = VideoTitie + "(" + Tag + ")[" + YYYY年MM月DD日HH時MM分SS秒 + "}.xml"
	 * 			投稿者コメント = VideoTitle + "(" + Tag + "){Owner].xml"
	 *
	 *
	 */

}
