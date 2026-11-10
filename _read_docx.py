import zipfile, re, sys, io, os, glob
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
files = glob.glob(r'E:\宠物\*.docx')
print("FOUND:", files, file=sys.stderr)
path = files[0]
with zipfile.ZipFile(path) as z:
    xml = z.read('word/document.xml').decode('utf-8')
paras = xml.split('</w:p>')
out = []
for p in paras:
    ts = re.findall(r'<w:t[^>]*>(.*?)</w:t>', p, re.S)
    line = ''.join(ts).strip()
    if line:
        out.append(line)
print('\n'.join(out))
