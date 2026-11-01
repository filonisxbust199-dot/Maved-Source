"""Export Fortnite diffuse maps instead of shader masks and oversized node graphs."""

from typing import NamedTuple


class MaterialPreparation(NamedTuple):
    material: str
    node_count: int
    simplified: bool
    color_image: str | None
    has_principled_output: bool


def _material_graph(material):
    visited = set()
    images = {}
    node_count = 0

    def visit(tree):
        nonlocal node_count
        if tree is None or tree.as_pointer() in visited:
            return
        visited.add(tree.as_pointer())
        node_count += len(tree.nodes)
        for node in tree.nodes:
            if node.type == "TEX_IMAGE" and node.image:
                images[node.image.as_pointer()] = node.image
            elif node.type == "GROUP" and node.node_tree:
                visit(node.node_tree)

    if material.use_nodes and material.node_tree:
        visit(material.node_tree)
    return node_count, list(images.values())


def _color_image(images):
    ranked = []
    for image in images:
        name = image.name.lower().rsplit(".", 1)[0]
        tokens = name.replace("-", "_").split("_")
        score = 0
        if "cl" in tokens or "bc" in tokens:
            score = 100
        elif "basecolor" in tokens or "base_color" in name:
            score = 95
        elif "albedo" in tokens or "diffuse" in tokens:
            score = 90
        elif "d" in tokens:
            score = 85
        if score and (getattr(image, "has_data", False) or image.packed_file or image.filepath):
            ranked.append((score, image.name.lower(), image))
    return sorted(ranked, key=lambda item: (-item[0], item[1]))[0][2] if ranked else None


def _diffuse_input(material):
    # Root shader parameters identify the material's diffuse, unlike unrelated defaults
    # buried inside reusable groups (wraps, iris maps, noise and thin-film gradients).
    names = {"litdiffusecolor": 110, "diffusecolor": 100,
             "basecolor": 100, "basecolortexturecolor": 100, "albedocolor": 100}
    candidates = []
    for link in material.node_tree.links:
        socket = "".join(c for c in link.to_socket.name.lower() if c.isalnum())
        score = names.get(socket, 0)
        source = link.from_node
        if score and source.type == "TEX_IMAGE" and source.image:
            candidates.append((score, source.image.name, source.image))
    return sorted(candidates, key=lambda item: (-item[0], item[1]))[0][2] if candidates else None


def _simplify(material, image):
    tree = material.node_tree
    tree.nodes.clear()
    output = tree.nodes.new("ShaderNodeOutputMaterial")
    shader = tree.nodes.new("ShaderNodeBsdfPrincipled")
    shader.inputs["Base Color"].default_value = tuple(material.diffuse_color)
    tree.links.new(shader.outputs["BSDF"], output.inputs["Surface"])
    if image is not None:
        texture = tree.nodes.new("ShaderNodeTexImage")
        texture.image = image
        tree.links.new(texture.outputs["Color"], shader.inputs["Base Color"])
        # Fortnite diffuse alpha often stores shader data, rather than opacity.
        shader.inputs["Alpha"].default_value = 1.0
    return True


def prepare_complex_materials(node_limit=512):
    """Make Fortnite albedo explicit; leave transparent helper materials untouched."""
    import bpy

    results = []
    for material in bpy.data.materials:
        if not (material.use_nodes and material.node_tree):
            continue
        node_count, images = _material_graph(material)
        if node_count <= node_limit and not material.name.startswith("FR_v2_"):
            continue
        image = _diffuse_input(material)
        if image is None:
            root_images = [node.image for node in material.node_tree.nodes
                           if node.type == "TEX_IMAGE" and node.image]
            image = _color_image(root_images) or _color_image(images)
        if image is None:
            continue
        _simplify(material, image)
        result = MaterialPreparation(material.name, node_count, True,
                                     image.name if image is not None else None, True)
        results.append(result)
        print("ESS: simplified material", material.name, "nodes", node_count,
              "base color", result.color_image or "solid material color")
    return results
