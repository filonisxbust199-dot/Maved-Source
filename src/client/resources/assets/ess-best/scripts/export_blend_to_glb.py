import bpy
import os
import sys
import traceback

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from blend_materials import prepare_complex_materials

arguments = sys.argv[sys.argv.index("--") + 1:]
if len(arguments) != 1:
    raise RuntimeError("Expected one output GLB path")

try:
    prepare_complex_materials()
    result = bpy.ops.export_scene.gltf(
        filepath=arguments[0],
        export_format="GLB",
        export_animations=True,
        export_skins=True,
        export_morph=True,
        export_morph_normal=True,
        export_morph_tangent=True,
        export_apply=False,
        export_yup=True,
    )
    if "FINISHED" not in result:
        raise RuntimeError("glTF exporter did not finish: " + str(result))
except BaseException:
    traceback.print_exc()
    sys.exit(1)
