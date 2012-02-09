#ifndef MAIN_H_
#define MAIN_H_
#include <SDL/SDL.h>
#include <SDL/SDL_ttf.h>
#include "nicodef.h"
#include "struct_define.h"
#include "chat/chat.h"
#include "chat/chat_slot.h"

struct CDATA{
	int enable_comment;
	CHAT chat;
	CHAT_SLOT slot;
};

struct DATA{
	FILE* log;
	TTF_Font* font[CMD_FONT_MAX];
	SDL_Surface* screen;
	/*���ꂼ��̃R�����g�ɉ������f�[�^*/
	//���[�U�R�����g
	CDATA user;
//	int enable_user_comment;
//	CHAT chat;
//	CHAT_SLOT slot;
	//���e�҃R�����g
	CDATA owner;
//	int enable_owner_comment;
//	CHAT ownerchat;
//	CHAT_SLOT ownerslot;
	//�I�v�V���i���R�����g
	CDATA optional;
//	int enable_optional_comment;
//	CHAT optionalchat;
//	CHAT_SLOT optionalslot;
	//��ʓI�ȃf�[�^
	int shadow_kind;
	int show_video;
	int fontsize_fix;
	int opaque_comment;
	int optional_trunslucent;
	int process_first_called;
	int video_length;
//	int aspect100;		// �A�X�y�N�g��*100	Not used now
	int nico_width_now;	// ������̉���
	float font_w_fix_r;	// �t�H���g�̕���nicoplayer.swf�ɍ��킹��{��(0< <2)�i�����I�j
	float font_h_fix_r;	// �t�H���g�̍�����nicoplayer.swf�ɍ��킹��{��(0< <2)�i�����I�j
	int original_resize;	// ������΂��Ǝ����T�C�Y���L���i�f�t�H���g�L���j
	int comment_speed;	// �R�����g���x���w�肷��ꍇ��0
	int enableCA;
	int use_lineskip_as_fontsize;	//�t�H���g�T�C�Y�����߂�̂�LineSkip�����킹��i�����I�j
	int debug;
	double width_scale;	//�������݉@videowidth/nicowidth_now
	// CA�p�t�H���g
	TTF_Font* CAfont[CA_FONT_MAX][CMD_FONT_MAX];
	// CA�ؑ֗pUnicode�Q
	//Uint16* font_change[CA_FONT_MAX];
	// 0:*protect_gothic
	// 1:*change_simsun
	// 2:*change_gulim
	// 3:*arial
	// 4:*gergia
	// 5:*msui_gothic
	// 6:*devanagari
	// 7:*extra
	Uint16* extra_change;
	//Uint16* zero_width;
	//Uint16* spaceable;
//	int limit_height;
	// �����I�ݒ聫
	short font_pixel_size[CMD_FONT_MAX];
	short fixed_font_height[CMD_FONT_MAX];	// �C���t�H���g�w��(�|�C���g�w��)
};

typedef struct SETTING{
	const char* data_user_path;
	const char* data_owner_path;
	const char* data_optional_path;
	const char* font_path;
	int video_length;
	// �V����ffmpeg����video�̎��Ԃ�ႤInterface��������܂ő����
	// ������΂�����n���i�A���s���̏ꍇ�́@0�@or�@-1�j
	// �R�����g�\���̍Ō�̒��������Ȃ̂łȂ��Ă��䖝����悤�ɕύX�B
	// ���K�v�Ȃ��Ȃ���
	int font_index;
	int user_slot_max;
	int owner_slot_max;
	int optional_slot_max;
	int shadow_kind;
	int nico_width_now;	// ������̉���
	/*TRUE OR FALSE*/
	int enable_user_comment;
	int enable_owner_comment;
	int enable_optional_comment;
	int show_video;
	int fontsize_fix;
	int opaque_comment;
	int optional_trunslucent;
	// CA�p�t�H���g
	const char* CAfont_path[CA_FONT_MAX];
	int CAfont_index[CA_FONT_MAX];
	// CA�ؑ֗pUnicode�Q
	//const char* CAfont_change_uc[CA_FONT_MAX];
	//const char* zero_width_uc;
	//const char* spaceable_uc;
	// �����p�ǉ��t�H���g
	const char* extra_path;
	const char* extra_uc;
	//int extra_fontindex;
	// �R�~���j�e�B����ł͒ʏ�R�����g���I�v�V���i���X���b�h�Ƃ��Ĕ������ɂ��Ă��邽�߁A�I���\�ɂ���
	float font_w_fix_r;	// �t�H���g�̕���nicoplayer.swf�ɍ��킹��{���i�����I�j
	float font_h_fix_r;	// �t�H���g�̍�����nicoplayer.swf�ɍ��킹��{���i�����I�j
	int original_resize;	// ������΂��Ǝ����T�C�Y���L���i�f�t�H���g�L���j
	int comment_speed;	// �R�����g���x���w�肷��ꍇ��0
	int enableCA;
	int use_lineskip_as_fontsize;	//�t�H���g�T�C�Y�����߂�̂�LineSkip�����킹��i�����I�j
	int debug;
}SETTING;

int init(FILE* log);
int initData(DATA* data,FILE* log,const SETTING* setting);
int main_process(DATA* data,SDL_Surface* surf,const int now_vpos);
int closeData(DATA* data);
int close();

#endif /*MAIN_H_*/