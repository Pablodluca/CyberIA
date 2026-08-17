import os
import re

for root, dirs, files in os.walk('.'):
    if '/build/' in root or '.gradle' in root:
        continue
    for file in files:
        if file.endswith('.xml'):
            ruta = os.path.join(root, file)
            with open(ruta, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Find the XML declaration if it exists
            xml_decl_match = re.search(r'<\?xml.*?\?>', content)
            if xml_decl_match:
                xml_decl = xml_decl_match.group(0)
                # Remove it from current place
                content = content.replace(xml_decl, '')
                # Put it at the very top
                content = xml_decl + '\n' + content.lstrip()
                
                with open(ruta, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Fixed XML declaration in {ruta}")
