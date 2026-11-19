"""Create a browser-applicable APK delta from matching 4 KiB blocks."""
import base64
import gzip
import hashlib
import json
import mmap
from pathlib import Path
import sys

base_path, target_path, output_path = map(Path, sys.argv[1:])
block_size = 4096
with base_path.open("rb") as base_file, target_path.open("rb") as target_file:
    with mmap.mmap(base_file.fileno(), 0, access=mmap.ACCESS_READ) as base:
        with mmap.mmap(target_file.fileno(), 0, access=mmap.ACCESS_READ) as target:
            blocks = {}
            for offset in range(0, len(base), block_size):
                block = base[offset:offset + block_size]
                blocks.setdefault(hashlib.blake2b(block, digest_size=16).digest(), offset)
            operations = []
            additions = bytearray()
            verified = hashlib.sha256()
            for offset in range(0, len(target), block_size):
                block = target[offset:offset + block_size]
                source_offset = blocks.get(hashlib.blake2b(block, digest_size=16).digest())
                if source_offset is not None and base[source_offset:source_offset + len(block)] == block:
                    operation = ["copy", source_offset, len(block)]
                    verified.update(base[source_offset:source_offset + len(block)])
                else:
                    operation = ["data", len(additions), len(block)]
                    additions.extend(block)
                    verified.update(block)
                if operations and operations[-1][0] == operation[0] and operations[-1][1] + operations[-1][2] == operation[1]:
                    operations[-1][2] += operation[2]
                else:
                    operations.append(operation)
            target_sha = hashlib.sha256(target).hexdigest()
            assert verified.hexdigest() == target_sha
            manifest = {
                "format": "verbatim-apk-delta-v1",
                "baseSize": len(base),
                "baseSha256": hashlib.sha256(base).hexdigest(),
                "targetSize": len(target),
                "targetSha256": target_sha,
                "filename": "Verbatim-Android-ARM64-Hotfix.apk",
                "operations": operations,
                "data": base64.b64encode(additions).decode("ascii"),
            }
            output_path.write_bytes(gzip.compress(json.dumps(manifest, separators=(",", ":")).encode(), mtime=0))
            print(json.dumps({
                "baseSize": len(base),
                "baseSha256": manifest["baseSha256"],
                "targetSize": len(target),
                "targetSha256": target_sha,
                "patchSize": output_path.stat().st_size,
                "operations": len(operations),
            }))
