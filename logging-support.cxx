void log (const char* text)
{
}

void log (const _TCHAR* text)
{
	const wchar_t *MsgItems[]=
	{
		TEXT("Debug"),
		text,
		L"\x01",                      /* separator line */
		TEXT("OK"),
	};

	Info.Message(&MainGuid,           /* GUID */
		nullptr,
		FMSG_WARNING|FMSG_LEFTALIGN,  /* Flags */
		L"Contents",                  /* HelpTopic */
		MsgItems,                     /* Items */
		ARRAYSIZE(MsgItems),          /* ItemsNumber */
		1);                           /* ButtonsNumber */

}

void log (const int number)
{
}

void log (LPCTSTR text, const int number)
{
}

void log (LPCTSTR text, LPCTSTR text1)
{
}

