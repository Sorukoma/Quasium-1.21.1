#!/usr/bin/env python3
"""Generate Heater and Calloud GUI textures for Quasium mod."""
from PIL import Image, ImageDraw
import os

# ─── Vanilla Color Palette (RGBA) ───
TRANSPARENT = (0, 0, 0, 0)
BLACK       = (0, 0, 0, 255)
WHITE       = (255, 255, 255, 255)
DARK_GRAY   = (55, 55, 55, 255)
MEDIUM_GRAY = (85, 85, 85, 255)
LIGHT_GRAY  = (139, 139, 139, 255)
BG_GRAY     = (198, 198, 198, 255)

# Fire gradient colors
FIRE_TOP    = (255, 140, 0, 255)   # Bright orange at top
FIRE_BOTTOM = (180, 60, 0, 255)    # Dark red-orange at bottom

SLOT_SIZE = 18

def draw_slot_border(draw, x, y):
    """Draw a single slot with 3D border effect (vanilla-authentic)."""
    # Outer black border (1px)
    draw.rectangle([x, y, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1], outline=BLACK)
    # Inner dark shadow (top-left, 1px inside border)
    for i in range(1, SLOT_SIZE - 1):
        draw.point((x + i, y + 1), fill=DARK_GRAY)    # Top edge
        draw.point((x + 1, y + i), fill=DARK_GRAY)    # Left edge
    # Inner light highlight (bottom-right, 1px inside border)
    for i in range(1, SLOT_SIZE - 1):
        draw.point((x + i, y + SLOT_SIZE - 2), fill=LIGHT_GRAY)  # Bottom edge
        draw.point((x + SLOT_SIZE - 2, y + i), fill=LIGHT_GRAY)  # Right edge
    # Interior (16x16)
    draw.rectangle([x + 2, y + 2, x + SLOT_SIZE - 3, y + SLOT_SIZE - 3], fill=BG_GRAY)


def draw_gui_background(draw, width, height):
    """Draw the main GUI background with 3D border effect."""
    # Outer black border (1px)
    draw.rectangle([0, 0, width - 1, 0], fill=BLACK)
    draw.rectangle([0, height - 1, width - 1, height - 1], fill=BLACK)
    draw.rectangle([0, 0, 0, height - 1], fill=BLACK)
    draw.rectangle([width - 1, 0, width - 1, height - 1], fill=BLACK)
    # White highlight (top/left, 1px inside)
    draw.rectangle([1, 1, width - 2, 1], fill=WHITE)
    draw.rectangle([1, 1, 1, height - 2], fill=WHITE)
    # Medium gray shadow (bottom/right, 1px inside)
    draw.rectangle([1, height - 2, width - 2, height - 2], fill=MEDIUM_GRAY)
    draw.rectangle([width - 2, 1, width - 2, height - 2], fill=MEDIUM_GRAY)
    # Fill background
    draw.rectangle([2, 2, width - 3, height - 3], fill=BG_GRAY)


def draw_player_inventory(draw, inv_x, inv_y, hotbar_x, hotbar_y):
    """Draw 9x3 player inventory + 9x1 hotbar."""
    # 3x9 inventory
    for row in range(3):
        for col in range(9):
            draw_slot_border(draw, inv_x + col * SLOT_SIZE, inv_y + row * SLOT_SIZE)
    # 1x9 hotbar
    for col in range(9):
        draw_slot_border(draw, hotbar_x + col * SLOT_SIZE, hotbar_y)


def draw_fire_indicator(draw, x, y, width, height):
    """Draw a fire/flame indicator with orange gradient."""
    for row in range(height):
        # Linear gradient from FIRE_TOP (top) to FIRE_BOTTOM (bottom)
        ratio = row / (height - 1) if height > 1 else 0
        r = int(FIRE_TOP[0] + (FIRE_BOTTOM[0] - FIRE_TOP[0]) * ratio)
        g = int(FIRE_TOP[1] + (FIRE_BOTTOM[1] - FIRE_TOP[1]) * ratio)
        b = int(FIRE_TOP[2] + (FIRE_BOTTOM[2] - FIRE_TOP[2]) * ratio)
        color = (r, g, b, 255)
        for col in range(width):
            draw.point((x + col, y + row), fill=color)


def generate_heater_gui():
    """Generate Heater GUI texture (176x166)."""
    WIDTH = 176
    HEIGHT = 166

    img = Image.new('RGBA', (WIDTH, HEIGHT), TRANSPARENT)
    draw = ImageDraw.Draw(img)

    draw_gui_background(draw, WIDTH, HEIGHT)

    # 5x3 charcoal grid (15 slots) starting at x=8, y=18
    GRID_COLS = 5
    GRID_ROWS = 3
    GRID_X = 8
    GRID_Y = 18
    for row in range(GRID_ROWS):
        for col in range(GRID_COLS):
            draw_slot_border(draw, GRID_X + col * SLOT_SIZE, GRID_Y + row * SLOT_SIZE)

    # Fuel slot at x=152, y=36
    draw_slot_border(draw, 152, 36)

    # Fire/flame indicator at x=152, y=56, 16px wide, 20px tall
    draw_fire_indicator(draw, 152, 56, 16, 20)

    # Player inventory: 9x3 at y=84, hotbar at y=142
    draw_player_inventory(draw, 8, 84, 8, 142)

    output_path = '/home/sorukoma/dev/Quasium-1.21.1/src/main/resources/assets/quasium/textures/gui/heater.png'
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, 'PNG')
    print(f"Generated Heater GUI: {output_path} ({WIDTH}x{HEIGHT})")
    return output_path


def generate_calloud_gui():
    """Generate Calloud GUI texture (176x166)."""
    WIDTH = 176
    HEIGHT = 166

    img = Image.new('RGBA', (WIDTH, HEIGHT), TRANSPARENT)
    draw = ImageDraw.Draw(img)

    draw_gui_background(draw, WIDTH, HEIGHT)

    # 4 charcoal slots centered horizontally at y=36
    # Total width = 4 * 18 = 72, centered in 176: (176 - 72) // 2 = 52
    CALLOUT_COLS = 4
    CALLOUT_X = (WIDTH - CALLOUT_COLS * SLOT_SIZE) // 2  # = 52
    CALLOUT_Y = 36
    for col in range(CALLOUT_COLS):
        draw_slot_border(draw, CALLOUT_X + col * SLOT_SIZE, CALLOUT_Y)

    # Player inventory: 9x3 at y=84, hotbar at y=142
    draw_player_inventory(draw, 8, 84, 8, 142)

    output_path = '/home/sorukoma/dev/Quasium-1.21.1/src/main/resources/assets/quasium/textures/gui/calloud.png'
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, 'PNG')
    print(f"Generated Calloud GUI: {output_path} ({WIDTH}x{HEIGHT})")
    return output_path


def verify_texture(path, name):
    """Verify the generated texture."""
    img = Image.open(path)
    print(f"\n--- {name} Verification ---")
    print(f"Size: {img.size}, Mode: {img.mode}")

    # Check that background is BG_GRAY at a known interior point
    # Check a slot interior pixel
    # Check transparent corner
    px = img.load()
    print(f"Top-left corner (should be transparent): {px[0, 0]}")
    print(f"Interior bg pixel (should be BG_GRAY): {px[88, 83]}")  # Center-ish area
    print(f"Slot interior at grid start+2,+2 (should be BG_GRAY): {px[10, 20]}")


if __name__ == '__main__':
    heater_path = generate_heater_gui()
    calloud_path = generate_calloud_gui()

    verify_texture(heater_path, "Heater")
    verify_texture(calloud_path, "Calloud")

    print("\nDone! Both GUI textures generated successfully.")
